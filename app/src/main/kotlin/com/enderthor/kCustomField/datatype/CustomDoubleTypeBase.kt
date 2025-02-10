package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import kotlin.math.abs

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val globalIndex: Int
) : DataTypeImpl(extension, datatype) {

    companion object {
        private const val CACHE_SIZE = 100  // Aumentamos para mantener más valores históricos
        private const val CACHE_TTL = 240000L  // 4 minutos
        private const val CACHE_CLEANUP_INTERVAL = 300000L  // 5 minutos
        private const val VALUE_THRESHOLD = 0.01  // Umbral para considerar valores iguales
        private const val MIN_HITS_TO_KEEP = 10  // Número mínimo de hits para mantener en caché
        private const val MAX_UNUSED_TIME = 600000L  // 10 minutos sin uso
        private val viewCache = LruCache<String, CachedView>(CACHE_SIZE)
    }

    private data class CachedView(
        val remoteViews: RemoteViews,
        val timestamp: Long = System.currentTimeMillis(),
        val firstValue: Double = 0.0,
        val secondValue: Double = 0.0,
        val hitCount: Int = 0,
        val lastUsed: Long = System.currentTimeMillis()
    ) {
        fun isValid(currentTime: Long = System.currentTimeMillis()): Boolean {
            val timeSinceCreation = currentTime - timestamp
            val timeSinceLastUse = currentTime - lastUsed

            return when {
                // Mantener si es frecuentemente usado
                hitCount > MIN_HITS_TO_KEEP -> timeSinceLastUse < MAX_UNUSED_TIME
                // Mantener si está dentro del TTL
                timeSinceCreation < CACHE_TTL -> true
                // Eliminar si no cumple ninguna condición
                else -> false
            }
        }

        fun valuesMatch(newFirstValue: Double, newSecondValue: Double): Boolean {
            return abs(firstValue - newFirstValue) < VALUE_THRESHOLD &&
                    abs(secondValue - newSecondValue) < VALUE_THRESHOLD
        }

        fun incrementHits(): CachedView = copy(
            hitCount = hitCount + 1,
            lastUsed = System.currentTimeMillis()
        )

        fun updateLastUsed(): CachedView = copy(
            lastUsed = System.currentTimeMillis()
        )
    }


    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time else RefreshTime.HALF.time

    private lateinit var viewjob: Job
    private lateinit var cacheCleanupJob: Job

    private fun generateCacheKey(
        fieldNumber: Int,
        isHorizontal: Boolean,
        firstValue: Double,
        secondValue: Double,
        firstValueRight: Double,
        secondValueRight: Double,
        firstField: DoubleFieldType,
        secondField: DoubleFieldType,
        firstIconColor: ColorProvider,
        secondIconColor: ColorProvider,
        firstColorZone: ColorProvider,
        secondColorZone: ColorProvider,
        windText: String,
        windDiff: Int,
        layout: FieldPosition,
        isDivider: Boolean
    ): String = buildString {
        append("${fieldNumber}_")
        append("${isHorizontal}_")
        append("${firstValue}_")
        append("${secondValue}_")
        append("${firstValueRight}_")
        append("${secondValueRight}_")
        append("${firstField.hashCode()}_")
        append("${secondField.hashCode()}_")
        append("${firstIconColor.hashCode()}_")
        append("${secondIconColor.hashCode()}_")
        append("${firstColorZone.hashCode()}_")
        append("${secondColorZone.hashCode()}_")
        append("${windText}_")
        append("${windDiff}_")
        append("${layout.name}_")
        append(isDivider)
    }

    private fun manageCacheEntry(
        cacheKey: String,
        currentView: RemoteViews,
        firstValue: Double,
        secondValue: Double,
        currentTime: Long = System.currentTimeMillis()
    ) {
        val existingEntry = viewCache.get(cacheKey)
        if (existingEntry != null && existingEntry.isValid(currentTime)) {
            viewCache.put(cacheKey, existingEntry.copy(timestamp = currentTime))
            Timber.d("Cache entry refreshed for key: $cacheKey")
        } else {
            viewCache.put(cacheKey, CachedView(
                remoteViews = currentView,
                timestamp = currentTime,
                firstValue = firstValue,
                secondValue = secondValue,
                hitCount = 1
            ))
            Timber.d("New cache entry created for key: $cacheKey")
        }
    }

    private fun clearViewCache() {
        viewCache.evictAll()
        Timber.d("View cache cleared")
    }

    private fun invalidateOldCache() {
        val currentTime = System.currentTimeMillis()
        val iterator = viewCache.snapshot().entries.iterator()
        var invalidatedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isValid(currentTime)) {
                viewCache.remove(entry.key)
                invalidatedCount++
            }
        }

        if (invalidatedCount > 0) {
            Timber.d("Invalidated $invalidatedCount cached views")
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
        val scope = CoroutineScope(Dispatchers.IO)

        // Limpiar caché al iniciar
        DataTypeCache.clearCaches()

        // Monitorizar caché periódicamente
       /* val monitorJob = scope.launch {
            while (isActive) {
                Timber.d("Double Type Cache Stats: ${DataTypeCache.getStats()}")
                delay(60000) // Monitorizar cada minuto
            }
        }
        */
        var index = globalIndex

        clearViewCache()

        cacheCleanupJob = scope.launch {
            while (isActive) {
                delay(CACHE_CLEANUP_INTERVAL)
                invalidateOldCache()
            }
        }

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        Timber.d("Starting Double view field $extension and index $index and field $dataTypeId")
        viewjob = scope.launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settingsFlow = context.streamDoubleFieldSettings().stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                listOf(DoubleFieldSettings())
            )

            val generalSettingsFlow = context.streamGeneralSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            var fieldNumber: Int = 3
            var clayout: FieldPosition = FieldPosition.CENTER
            val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

            retryFlow(
                action = {
                    val settingsFlow = combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                        .firstOrNull { (settings, _) -> index in settings.indices }

                    if (settingsFlow != null) {
                        Timber.d("DOUBLE INITIAL RETRYFLOW encontrado: $index  campo: $dataTypeId")
                        val (settings, generalSettings) = settingsFlow
                        emit(settings to generalSettings)
                    } else {
                        Timber.e("DOUBLE INITIAL index out of Bounds: $index campo: $dataTypeId")
                        throw IndexOutOfBoundsException("Index out of Bounds")
                    }
                },
                onFailure = { attempts, e ->
                    Timber.e("DOUBLE INITIAL index not valid in $attempts attemps. Error: $e")
                    emit(listOf(DoubleFieldSettings()) to GeneralSettings())
                }
            ).collectLatest { (settings, generalSettings) ->
                if (index in settings.indices) {
                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

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
                        ishorizontal(settings[index]) -> generalSettings.iscenteralign
                        else -> generalSettings.iscentervertical
                    }

                    if (!config.preview) {
                        val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                            DoubleScreenSelector(
                                fieldNumber,
                                ishorizontal(settings[index]),
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
                    val currentSetting = settings[index]
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
                    val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(
                        firstFieldState,
                        firstField(settings[index]),
                        context,
                        userProfile,
                        generalSettings.ispalettezwift
                    )

                    val (secondvalue, secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldState(
                        secondFieldState,
                        secondField(settings[index]),
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
                        ishorizontal(settings[index]) -> generalSettings.iscenteralign
                        else -> generalSettings.iscentervertical
                    }

                    val cacheKey = generateCacheKey(
                        fieldNumber,
                        ishorizontal(settings[index]),
                        firstvalue,
                        secondvalue,
                        firstvalueRight,
                        secondvalueRight,
                        firstField(settings[index]),
                        secondField(settings[index]),
                        firstIconcolor,
                        secondIconcolor,
                        firstColorzone,
                        secondColorzone,
                        windtext,
                        winddiff.roundToInt(),
                        clayout,
                        generalSettings.isdivider
                    )

                    val cachedView = viewCache.get(cacheKey)?.let { cached ->
                        if (cached.isValid() && cached.valuesMatch(firstvalue, secondvalue)) {
                            val updatedCache = if (abs(System.currentTimeMillis() - cached.lastUsed) > refreshTime) {
                                cached.incrementHits()
                            } else {
                                cached.updateLastUsed()
                            }
                            viewCache.put(cacheKey, updatedCache)
                            Timber.d("Cache hit for key: $cacheKey, hits: ${updatedCache.hitCount}")
                            cached.remoteViews
                        } else null
                    }


                    if (cachedView != null) {
                        emitter.updateView(cachedView)
                    } else {
                        val newView = glance.compose(context, DpSize.Unspecified) {
                            DoubleScreenSelector(
                                fieldNumber,
                                ishorizontal(settings[index]),
                                firstvalue,
                                secondvalue,
                                firstField(settings[index]),
                                secondField(settings[index]),
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

                        manageCacheEntry(
                            cacheKey,
                            newView,
                            firstvalue,
                            secondvalue
                        )
                        emitter.updateView(newView)
                    }
                }
                .retryWhen { cause, attempt ->
                    if (attempt > 4) {
                        Timber.e(cause, "Error collecting Double flow, stopping.. (attempt $attempt) Cause: $cause")
                        scope.cancel()
                        configJob.cancel()
                        viewjob.cancel()
                        cacheCleanupJob.cancel()
                        clearViewCache()
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
        }

        emitter.setCancellable {
            Timber.d("Stopping Double view with $emitter")
            configJob.cancel()
            viewjob.cancel()
            cacheCleanupJob.cancel()
            clearViewCache()
        }
    }
}
