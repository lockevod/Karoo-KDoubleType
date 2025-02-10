package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import com.enderthor.kCustomField.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import timber.log.Timber
import kotlin.math.abs

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomRollingTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val index: Int
) : DataTypeImpl(extension, datatype) {

    companion object {
        private const val CACHE_SIZE = 50
        private const val MIN_HITS_TO_KEEP = 5  // Menor que Double porque rota más rápido
        private val viewCache = LruCache<String, CachedView>(CACHE_SIZE)
    }

    private data class CachedView(
        val remoteViews: RemoteViews,
        val timestamp: Long = System.currentTimeMillis(),
        val value: Double = 0.0,
        val valueSecond: Double = 0.0,
        val hitCount: Int = 0,
        val lastUsed: Long = System.currentTimeMillis()
    ) {
        fun isValid(rollingTime: Long, currentTime: Long = System.currentTimeMillis()): Boolean {
            val timeSinceCreation = currentTime - timestamp
            val timeSinceLastUse = currentTime - lastUsed

            return when {
                // Mantener si es frecuentemente usado y dentro del tiempo de rolling
                hitCount > MIN_HITS_TO_KEEP -> timeSinceLastUse < rollingTime * 2
                // Mantener si está dentro del tiempo de rolling
                timeSinceCreation < rollingTime -> true
                // Eliminar si no cumple ninguna condición
                else -> false
            }
        }

        fun valuesMatch(newValue: Double, newValueSecond: Double): Boolean {
            return abs(value - newValue) < 0.01 && abs(valueSecond - newValueSecond) < 0.01
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
    protected val firstField = { settings: OneFieldSettings -> settings.onefield }
    protected val secondField = { settings: OneFieldSettings -> settings.secondfield }
    protected val thirdField = { settings: OneFieldSettings -> settings.thirdfield }
    protected val rollingtime = { settings: OneFieldSettings -> settings.rollingtime }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time + RefreshTime.EXTRA_ROLLING.time else RefreshTime.HALF.time + RefreshTime.EXTRA_ROLLING.time

    private lateinit var viewjob: Job
    private lateinit var cacheCleanupJob: Job

    private fun generateCacheKey(
        value: Double,
        valueSecond: Double,
        field: OneFieldType,
        iconColor: ColorProvider,
        colorZone: ColorProvider,
        fieldSize: FieldSize,
        isKaroo3: Boolean,
        layout: FieldPosition,
        windText: String,
        windDiff: Int,
        selector: Boolean,
        textSize: Int,
        isZone: Boolean,
        cyclicIndex: Int
    ): String {
        return buildString {
            append("${value}_")
            append("${valueSecond}_")
            append("${field.hashCode()}_")
            append("${iconColor.hashCode()}_")
            append("${colorZone.hashCode()}_")
            append("${fieldSize.name}_")
            append("${isKaroo3}_")
            append("${layout.name}_")
            append("${windText}_")
            append("${windDiff}_")
            append("${selector}_")
            append("${textSize}_")
            append("${isZone}_")
            append(cyclicIndex)
        }
    }

    private fun clearViewCache() {
        viewCache.evictAll()
        Timber.d("Rolling view cache cleared")
    }

    private fun invalidateOldCache(rollingTime: Long) {
        val currentTime = System.currentTimeMillis()
        val iterator = viewCache.snapshot().entries.iterator()
        var invalidatedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isValid(rollingTime, currentTime)) {
                viewCache.remove(entry.key)
                invalidatedCount++
            }
        }

        if (invalidatedCount > 0) {
            Timber.d("Invalidated $invalidatedCount rolling cached views")
        }
    }


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("Starting Rolling type stream")
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
                Timber.e(e, "Stream Rolling error occurred")
                emitter.onError(e)
            }
        }.also { job ->
            emitter.setCancellable {
                Timber.d("Stopping Rolling stream")
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
        val monitorJob = scope.launch {
            while (isActive) {
                Timber.d("Rolling Type Cache Stats: ${DataTypeCache.getStats()}")
                delay(60000) // Monitorizar cada minuto
            }
        }

        val globalIndex = index

        clearViewCache()

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)
        Timber.d("Starting ROLLING view field $extension and index $index and field $dataTypeId")
        viewjob = scope.launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settings = context.streamOneFieldSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), listOf(OneFieldSettings()))

            val generalSettings = context.streamGeneralSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            // Setup cache cleanup job based on rolling time
            settings.first().getOrNull(globalIndex)?.let { setting ->
                cacheCleanupJob = scope.launch {
                    while (isActive) {
                        val currentTTL = rollingtime(setting).time
                        delay(currentTTL / 2)
                        invalidateOldCache(currentTTL)
                    }
                }
            }

            retryFlow(
                action = {
                    val settingsFlow = combine(settings, generalSettings) { settings, generalSettings -> settings to generalSettings }
                        .firstOrNull { (settings, _) -> globalIndex in settings.indices }

                    if (settingsFlow != null) {
                        Timber.d("DOUBLE INITIAL RETRYFLOW encontrado: $index  campo: $dataTypeId" )
                        val (settings, generalSettings) = settingsFlow
                        emit(settings to generalSettings)
                    } else {
                        Timber.e("GlobalIndex InitView Rolling out of bounds Index : $globalIndex")
                        throw IndexOutOfBoundsException("GlobalIndex out of bounds")
                    }
                },
                onFailure = { attempts, e ->
                    Timber.e("Not valid Rolling index  in $attempts attemps. Error: $e")
                    emit(listOf(OneFieldSettings()) to GeneralSettings())
                }
            ).collectLatest { (settings, generalSettings) ->
                if (globalIndex in settings.indices) {
                    val initField = firstField(settings[globalIndex])
                    val initSelector =
                        !(initField.kaction.name == "HEADWIND" && generalSettings.isheadwindenabled)
                    if (!config.preview) {
                        val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                            RollingFieldScreen(
                                0.0,
                                !(initField.kaction.convert == "speed" || initField.kaction.zone == "slopeZones" || initField.kaction.label == "IF"),
                                initField.kaction,
                                ColorProvider(Color.Black, Color.White),
                                ColorProvider(Color.White, Color.Black),
                                getFieldSize(config.gridSize.second),
                                karooSystem.hardwareType == HardwareType.KAROO,
                                generalSettings.iscenteralign,
                                "",
                                0,
                                baseBitmap,
                                initSelector,
                                config.textSize,
                                false,
                                config.preview,
                                0.0
                            )
                        }.remoteViews
                        emitter.updateView(initialRemoteViews)
                    }
                } else
                    Timber.e("GlobalIndex InitView Rolling fuera de los límites: $globalIndex, Tamaño: ${settings.size}")
            }

            val cyclicIndexFlow = settings.flatMapLatest { settings ->
                if (settings.isNotEmpty() && globalIndex in settings.indices && rollingtime(settings[globalIndex]).time > 0L) {
                    flow {
                        var cyclicindex = 0
                        val currentSetting = settings[globalIndex]
                        while (true) {
                            emit(cyclicindex)
                            cyclicindex = when (cyclicindex) {
                                0 -> if (secondField(currentSetting).isactive) 1 else if (thirdField(currentSetting).isactive) 2 else 0
                                1 -> if (thirdField(currentSetting).isactive) 2 else 0
                                else -> 0
                            }
                            delay(rollingtime(currentSetting).time)
                        }
                    }.flowOn(Dispatchers.IO).distinctUntilChanged().catch { e ->
                        Timber.e(e, "Error in cyclicIndexFlow")
                        emit(0)
                    }
                } else {
                    flowOf(0)
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), 0)

            val combinedFlow = combine(settings, generalSettings, cyclicIndexFlow) { settings, generalSettings, cyclicIndex ->
                Triple(settings, generalSettings, cyclicIndex)
            }

            combinedFlow
                .flatMapLatest { (settings, generalSetting, cyclicIndex) ->
                    val currentSetting = settings.getOrNull(globalIndex) ?: return@flatMapLatest flowOf(Triple(previewOneFieldSettings, GeneralSettings(), 0) to Triple(settings, generalSetting, cyclicIndex))
                    Timber.d("ROLLING FLAT: $index datos: ${settings[index]} campo: $dataTypeId" )
                    val primaryField = firstField(currentSetting)
                    val secondaryField = secondField(currentSetting)
                    val thirdField = thirdField(currentSetting)

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField, thirdField).any { it.kaction.name == "HEADWIND" } && generalSetting.isheadwindenabled && !config.preview)
                            createHeadwindFlow(karooSystem, refreshTime) else flowOf(StreamHeadWindData(0.0, 0.0))

                    val firstFieldFlow = if (!config.preview) getFieldFlow(karooSystem, primaryField, headwindFlow, generalSetting, refreshTime) else previewFlow()
                    val secondFieldFlow = if (!config.preview) getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSetting, refreshTime) else previewFlow()
                    val thirdFieldFlow = if (!config.preview) getFieldFlow(karooSystem, thirdField, headwindFlow, generalSetting, refreshTime) else previewFlow()

                    combine(firstFieldFlow, secondFieldFlow, thirdFieldFlow) { firstField, secondField, thirdField ->
                        Triple(firstField, secondField, thirdField)
                    }.map { (firstFieldState, secondFieldState, thirdFieldState) ->
                        Triple(firstFieldState, secondFieldState, thirdFieldState) to Triple(settings, generalSetting, cyclicIndex)
                    }.catch { e ->
                        Timber.e(e, "Error in Rolling combined flow")
                    }
                }
                .debounce(refreshTime)
                .onEach { (fieldStates, settingsData) ->
                    if (globalIndex !in settingsData.first.indices) {
                        Timber.e("GlobalIndex fuera de los límites: $globalIndex, Tamaño: ${settingsData.first.size}")
                        return@onEach
                    }

                    val (firstFieldState, secondFieldState, thirdFieldState) = fieldStates
                    val (settings, generalSetting, cyclicIndex) = settingsData

                    val field = when (cyclicIndex) {
                        0 -> firstField
                        1 -> secondField
                        2 -> thirdField
                        else -> firstField
                    }

                    val valuestream = when (cyclicIndex) {
                        0 -> firstFieldState
                        1 -> secondFieldState
                        2 -> thirdFieldState
                        else -> firstFieldState
                    }

                    val (value, iconcolor, colorzone, iszone, valueSecond) = getFieldState(
                        valuestream,
                        field(settings[globalIndex]),
                        context,
                        userProfile,
                        generalSetting.ispalettezwift
                    )

                    val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState || thirdFieldState !is StreamState) {
                        val windData = (firstFieldState as? StreamHeadWindData)
                            ?: (secondFieldState as? StreamHeadWindData)
                            ?: (thirdFieldState as StreamHeadWindData)
                        windData.diff to windData.windSpeed.roundToInt().toString()
                    } else 0.0 to ""

                    val selector: Boolean = valuestream is StreamState

                    val cacheKey = generateCacheKey(
                        value,
                        valueSecond,
                        field(settings[globalIndex]),
                        iconcolor,
                        colorzone,
                        getFieldSize(config.gridSize.second),
                        karooSystem.hardwareType == HardwareType.KAROO,
                        generalSetting.iscenteralign,
                        windtext,
                        winddiff.roundToInt(),
                        selector,
                        config.textSize,
                        iszone,
                        cyclicIndex
                    )

                    val cachedView = viewCache.get(cacheKey)?.let { cached ->
                        val rollingTime = rollingtime(settings[globalIndex]).time
                        if (cached.isValid(rollingTime) && cached.valuesMatch(value, valueSecond)) {
                            val updatedCache = if (System.currentTimeMillis() - cached.lastUsed > refreshTime) {
                                cached.incrementHits()
                            } else {
                                cached.updateLastUsed()
                            }
                            viewCache.put(cacheKey, updatedCache)
                            Timber.d("Rolling cache hit for key: $cacheKey, hits: ${updatedCache.hitCount}")
                            cached.remoteViews
                        } else null
                    }

                    if (cachedView != null) {
                        emitter.updateView(cachedView)
                    } else {
                        val newView = glance.compose(context, DpSize.Unspecified) {
                            RollingFieldScreen(
                                value,
                                !(field(settings[globalIndex]).kaction.convert == "speed" ||
                                        field(settings[globalIndex]).kaction.zone == "slopeZones" ||
                                        field(settings[globalIndex]).kaction.label == "IF"),
                                field(settings[globalIndex]).kaction,
                                iconcolor,
                                colorzone,
                                getFieldSize(config.gridSize.second),
                                karooSystem.hardwareType == HardwareType.KAROO,
                                generalSetting.iscenteralign,
                                windtext,
                                winddiff.roundToInt(),
                                baseBitmap,
                                selector,
                                config.textSize,
                                iszone,
                                config.preview,
                                valueSecond
                            )
                        }.remoteViews

                        viewCache.put(cacheKey, CachedView(
                            remoteViews = newView,
                            value = value,
                            valueSecond = valueSecond
                        ))
                        Timber.d("Rolling cache miss - New view created and cached with key: $cacheKey")
                        emitter.updateView(newView)
                    }

                }
                .retryWhen { cause, attempt ->
                    if (attempt > 3) {
                        Timber.e(cause, "Error collecting Rolling flow, stopping.. (attempt $attempt) Cause: $cause")
                        scope.cancel()
                        configJob.cancel()
                        viewjob.cancel()
                        cacheCleanupJob.cancel()
                        clearViewCache()
                        delay(Delay.RETRY_LONG.time)
                        startView(context, config, emitter)
                        false
                    } else {
                        Timber.e(cause, "Error collecting Rolling flow, retrying... (attempt $attempt) Cause: $cause")
                        delay(Delay.RETRY_SHORT.time)
                        true
                    }
                }
                .launchIn(scope)
        }

        emitter.setCancellable {
            Timber.d("Stopping Rolling view with $emitter")
            configJob.cancel()
            viewjob.cancel()
            cacheCleanupJob.cancel()
            monitorJob.cancel() // Cancelar monitorización
            clearViewCache()
            DataTypeCache.clearCaches() // Limpiar caché al detener
        }
    }
}
