package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import android.util.LruCache
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.color.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamDoubleFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val globalIndex: Int
) : DataTypeImpl(extension, datatype) {

    companion object {
        private const val CACHE_SIZE = 100
        private const val CACHE_TTL = 240000L  // 4 minutos
        private const val CACHE_CLEANUP_INTERVAL = 300000L  // 5 minutos
        private const val INITIAL_CACHE_DELAY = 180000L // 3 minutos
        private val startTime = System.currentTimeMillis()
        private val viewCache = LruCache<String, CachedView>(CACHE_SIZE)
    }

    private data class CachedView(
        val remoteViews: RemoteViews,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime - timestamp < CACHE_TTL
        }
    }

    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time else RefreshTime.HALF.time

    private var viewjob: Job? = null
    private var cacheCleanupJob: Job? = null
    private val isInitialized = AtomicBoolean(false)

    private fun isCacheEnabled(): Boolean {
        return System.currentTimeMillis() - startTime > INITIAL_CACHE_DELAY
    }

    private fun clearViewCache() {
        if (isCacheEnabled()) {
            viewCache.evictAll()
            Timber.d("View cache cleared")
        }
    }

    private fun invalidateOldCache() {
        if (!isCacheEnabled()) return

        val iterator = viewCache.snapshot().entries.iterator()
        var invalidatedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isValid()) {
                viewCache.remove(entry.key)
                invalidatedCount++
            }
        }

        if (invalidatedCount > 0) {
            Timber.d("Invalidated $invalidatedCount cached views")
        }
    }

    private fun cleanupJobs() {
        try {
            viewjob?.cancel()
            viewjob = null

            cacheCleanupJob?.cancel()
            cacheCleanupJob = null

            isInitialized.set(false)
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up jobs")
        }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("Starting double type stream")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (true) {
                    emitter.onNext(StreamState.Streaming(
                        DataPoint(
                            dataTypeId,
                            mapOf(DataType.Field.SINGLE to 1.0),
                            extension
                        )
                    ))
                    delay(refreshTime)
                }
            } catch (e: Exception) {
                Timber.e(e, "Stream error occurred")
                emitter.onError(e)
            }
        }.also { job ->
            emitter.setCancellable {
                Timber.d("Stopping stream")
                job.cancel()
            }
        }
    }

    private fun previewFlow(): Flow<StreamState> = flow {
        while (true) {
            emit(StreamState.Streaming(
                DataPoint(
                    dataTypeId,
                    mapOf(DataType.Field.SINGLE to (0..100).random().toDouble()),
                    extension
                )
            ))
            delay(Delay.PREVIEW.time)
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("DOUBLE StartView: field $extension index $globalIndex field $dataTypeId config: $config")

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        cleanupJobs()
        DataTypeCache.clearCaches()
        clearViewCache()

        cacheCleanupJob = scope.launch {
            try {
                while (isActive) {
                    try {
                        delay(CACHE_CLEANUP_INTERVAL)
                        invalidateOldCache()
                    } catch (e: CancellationException) {
                        // La cancelación es esperada, no necesitamos logging
                        break
                    } catch (e: Exception) {
                        Timber.e(e, "Unexpected error in cache cleanup cycle")
                    }
                }
            } catch (e: CancellationException) {
                // Cancelación normal del job, no necesitamos logging
            } catch (e: Exception) {
                Timber.e(e, "Error in cache cleanup job")
            }
        }

        val configJob = scope.launch {
            try {
                emitter.onNext(UpdateGraphicConfig(showHeader = false))
                awaitCancellation()
            } catch (e: Exception) {
                Timber.e(e, "Error in config job")
            }
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

        viewjob = scope.launch {
            try {
                val userProfile = karooSystem.consumerFlow<UserProfile>().first()
                val settingsFlow = context.streamDoubleFieldSettings().stateIn(
                    scope,
                    SharingStarted.WhileSubscribed(5000),
                    listOf(DoubleFieldSettings())
                )

                val generalSettingsFlow = context.streamGeneralSettings()
                    .stateIn(scope, SharingStarted.WhileSubscribed(5000), GeneralSettings())

                isInitialized.set(true)

                var fieldNumber: Int = 3
                var clayout: FieldPosition = FieldPosition.CENTER

                retryFlow(
                    action = {
                        val settingsFlow = combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                            .firstOrNull { (settings, _) -> globalIndex in settings.indices }

                        if (settingsFlow != null) {
                            Timber.d("DOUBLE INITIAL RETRYFLOW encontrado: $globalIndex  campo: $dataTypeId")
                            val (settings, generalSettings) = settingsFlow
                            emit(settings to generalSettings)
                        } else {
                            Timber.e("DOUBLE INITIAL index out of Bounds: $globalIndex")
                            throw IndexOutOfBoundsException("Index out of Bounds")
                        }
                    },
                    onFailure = { attempts, e ->
                        Timber.e("DOUBLE INITIAL index not valid in $attempts attemps. Error: $e")
                        emit(listOf(DoubleFieldSettings()) to GeneralSettings())
                    }
                ).collectLatest { (settings, generalSettings) ->
                    if (globalIndex in settings.indices) {
                        val primaryField = firstField(settings[globalIndex])
                        val secondaryField = secondField(settings[globalIndex])

                        fieldNumber = when {
                            generalSettings.isheadwindenabled -> when {
                                primaryField.kaction.name == "HEADWIND" -> if (secondaryField.kaction.name == "HEADWIND") 2 else 0
                                secondaryField.kaction.name == "HEADWIND" -> 1
                                else -> 3
                            }
                            else -> 3
                        }

                        clayout = when {
                            fieldNumber != 3 -> FieldPosition.CENTER
                            generalSettings.iscenterkaroo -> when (config.alignment) {
                                ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                            }
                            ishorizontal(settings[globalIndex]) -> generalSettings.iscenteralign
                            else -> generalSettings.iscentervertical
                        }

                        if (!config.preview) {
                            val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                                DoubleScreenSelector(
                                    fieldNumber,
                                    ishorizontal(settings[globalIndex]),
                                    0.0,
                                    0.0,
                                    primaryField,
                                    secondaryField,
                                    ColorProvider(Color.Black, Color.White),
                                    ColorProvider(Color.Black, Color.White),
                                    ColorProvider(Color.White, Color.Black),
                                    ColorProvider(Color.White, Color.Black),
                                    getFieldSize(config.gridSize.second),
                                    karooSystem.hardwareType == HardwareType.KAROO,
                                    clayout,
                                    "",
                                    0,
                                    baseBitmap,
                                    generalSettings.isdivider
                                )
                            }.remoteViews
                            emitter.updateView(initialRemoteViews)
                        }
                    }
                }

                combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                    .flatMapLatest { (settings, generalSettings) ->
                        val currentSetting = settings[globalIndex]
                        val primaryField = firstField(currentSetting)
                        val secondaryField = secondField(currentSetting)

                        val headwindFlow =
                            if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                                createHeadwindFlow(karooSystem, refreshTime) else flowOf(StreamHeadWindData(0.0, 0.0))

                        val firstFieldFlow = if (!config.preview) getFieldFlow(karooSystem, primaryField, headwindFlow, generalSettings, refreshTime) else previewFlow()
                        val secondFieldFlow = if (!config.preview) getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSettings, refreshTime) else previewFlow()

                        combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                            Quadruple(firstState, secondState, settings, generalSettings)
                        }
                    }
                    .debounce(refreshTime)
                    .onEach { (firstFieldState, secondFieldState, settings, generalSettings) ->
                        if (!isInitialized.get()) {
                            Timber.w("DOUBLE Skip update - not initialized: $extension $globalIndex")
                            return@onEach
                        }

                        val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(
                            firstFieldState,
                            firstField(settings[globalIndex]),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )

                        val (secondvalue, secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldState(
                            secondFieldState,
                            secondField(settings[globalIndex]),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )

                        val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                            val windData = (firstFieldState as? StreamHeadWindData)
                                ?: (secondFieldState as StreamHeadWindData)
                            windData.diff to windData.windSpeed.roundToInt().toString()
                        } else 0.0 to ""

                        fieldNumber = when {
                            firstFieldState is StreamState && secondFieldState is StreamState -> 3
                            firstFieldState is StreamState -> 0
                            secondFieldState is StreamState -> 1
                            else -> 2
                        }

                        clayout = when {
                            fieldNumber != 3 -> FieldPosition.CENTER
                            generalSettings.iscenterkaroo -> when (config.alignment) {
                                ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                            }
                            ishorizontal(settings[globalIndex]) -> generalSettings.iscenteralign
                            else -> generalSettings.iscentervertical
                        }

                        try {
                            val newView = glance.compose(context, DpSize.Unspecified) {
                                DoubleScreenSelector(
                                    fieldNumber,
                                    ishorizontal(settings[globalIndex]),
                                    firstvalue,
                                    secondvalue,
                                    firstField(settings[globalIndex]),
                                    secondField(settings[globalIndex]),
                                    firstIconcolor,
                                    secondIconcolor,
                                    firstColorzone,
                                    secondColorzone,
                                    getFieldSize(config.gridSize.second),
                                    karooSystem.hardwareType == HardwareType.KAROO,
                                    clayout,
                                    windtext,
                                    winddiff.roundToInt(),
                                    baseBitmap,
                                    generalSettings.isdivider,
                                    firstvalueRight,
                                    secondvalueRight
                                )
                            }.remoteViews

                            Timber.d("DOUBLE Updating view: $extension $globalIndex values: $firstvalue, $secondvalue layout: $clayout")
                            emitter.updateView(newView)
                        } catch (e: Exception) {
                            Timber.e(e, "DOUBLE Error composing/updating view: $extension $globalIndex")
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "DOUBLE Flow error: $extension $globalIndex")
                    }
                    .retryWhen { cause, attempt ->
                        if (attempt > 3) {
                            Timber.e(cause, "Error collecting Double flow, stopping.. (attempt $attempt) Cause: $cause")
                            cleanupJobs()
                            delay(Delay.RETRY_LONG.time)
                            startView(context, config, emitter)
                            false
                        } else {
                            Timber.e(cause, "Error collecting Double flow, retrying... (attempt $attempt) Cause: $cause")
                            delay(Delay.RETRY_SHORT.time)
                            true
                        }
                    }
                    .launchIn(scope)

            } catch (e: Exception) {
                Timber.e(e, "DOUBLE ViewJob error: $extension $globalIndex")
                cleanupJobs()
                delay(1000)
                startView(context, config, emitter)
            }
        }

        emitter.setCancellable {
            Timber.d("DOUBLE Stopping view: $extension $globalIndex")
            cleanupJobs()
            configJob.cancel()
            DataTypeCache.clearCaches()
            clearViewCache()
        }
    }
}