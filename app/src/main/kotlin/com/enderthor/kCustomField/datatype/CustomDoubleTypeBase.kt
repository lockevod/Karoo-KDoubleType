package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.StateFlow
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


    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time else RefreshTime.HALF.time

    private var viewjob: Job? = null
    private var configJob: Job? = null
    private val isInitialized = AtomicBoolean(false)
    private lateinit var emitterId: String



    private fun cleanupJobs() {
        try {
            isInitialized.set(false)

            viewjob?.let {
                if (it.isActive) {
                    it.cancel()
                    Timber.d("DOUBLE ViewJob cancelled: $extension $globalIndex ViewEmitter@$emitterId")
                }
            }
            viewjob = null


            configJob?.let {
                if (it.isActive) {
                    it.cancel()
                    Timber.d("DOUBLE ConfigJob cancelled: $extension $globalIndex ViewEmitter@$emitterId")
                }
            }
            configJob = null

        } catch (e: Exception) {
            Timber.e(e, "DOUBLE Error cleaning up jobs: $extension $globalIndex ViewEmitter@$emitterId")
        }
    }

    private suspend fun initializeView(
        scope: CoroutineScope,
        context: Context
    ): Triple<UserProfile, StateFlow<List<DoubleFieldSettings>>, StateFlow<GeneralSettings>> {
        return try {
            Triple(
                karooSystem.consumerFlow<UserProfile>().first(),
                context.streamDoubleFieldSettings().stateIn(
                    scope,
                    SharingStarted.WhileSubscribed(5000),
                    listOf(DoubleFieldSettings())
                ),
                context.streamGeneralSettings().stateIn(
                    scope,
                    SharingStarted.WhileSubscribed(5000),
                    GeneralSettings()
                )
            ).also {
                isInitialized.set(true)
                Timber.d("DOUBLE View initialized: $extension $globalIndex ViewEmitter@$emitterId")
            }
        } catch (e: CancellationException) {
            Timber.d("DOUBLE Initialization cancelled: $extension $globalIndex ViewEmitter@$emitterId")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "DOUBLE Initialization error: $extension $globalIndex ViewEmitter@$emitterId")
            throw e
        }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("DOUBLE Starting stream: $extension $globalIndex")
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
                    delay(refreshTime-200L)
                }
            } catch (e: CancellationException) {
                Timber.d("DOUBLE Stream cancelled: $extension $globalIndex ViewEmitter@$emitterId")
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Stream error: $extension $globalIndex ViewEmitter@$emitterId")
                emitter.onError(e)
            }
        }.also { job ->
            emitter.setCancellable {
                Timber.d("DOUBLE Stopping stream: $extension $globalIndex ViewEmitter@$emitterId")
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
        Timber.d("DOUBLE StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")
        emitterId = emitter.toString().substringAfter("@")
        val scope = CoroutineScope(Dispatchers.IO)

        //cleanupJobs()

        configJob = scope.launch {
            try {
                emitter.onNext(UpdateGraphicConfig(showHeader = false))
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    // Cancelación normal
                }
            } catch (e: CancellationException) {
                // Cancelación normal del job
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Error in config job: $extension $globalIndex ViewEmitter@$emitterId")
            }
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

        viewjob = scope.launch {
            try {
                Timber.d("DOUBLE Starting view: $extension $globalIndex ViewEmitter@$emitterId")
                val (userProfile, settingsFlow, generalSettingsFlow) = initializeView(scope, context)

                var fieldNumber: Int = 3
                var clayout: FieldPosition = FieldPosition.CENTER

                try {

                    // Carga inicial rápida
                    if (!config.preview) {
                            try {
                                val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                                    DoubleScreenSelector(
                                        3, // Valor por defecto
                                        true, // horizontal por defecto
                                        0.0,
                                        0.0,
                                        firstField(settingsFlow.value[0]),
                                        secondField(settingsFlow.value[0]),
                                        ColorProvider(Color.Black, Color.White),
                                        ColorProvider(Color.Black, Color.White),
                                        ColorProvider(Color.White, Color.Black),
                                        ColorProvider(Color.White, Color.Black),
                                        getFieldSize(config.gridSize.second),
                                        karooSystem.hardwareType == HardwareType.KAROO,
                                        FieldPosition.CENTER, // Posición por defecto
                                        "",
                                        0,
                                        baseBitmap,
                                        true,
                                        0.0,
                                        0.0,
                                        true// divider desactivado inicialmente
                                    )
                                }.remoteViews
                                emitter.updateView(initialRemoteViews)

                                // Esperar 2 segundos antes de continuar con el resto
                                delay((800L..2000L).random())
                            } catch (e: Exception) {
                                Timber.e(e, "DOUBLE Error en vista inicial: $extension $globalIndex ViewEmitter@$emitterId")
                            }
                       // Esperar a que termine la vista inicial
                    }

                    retryFlow(
                        action = {
                            val settingsFlow = combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                                .firstOrNull { (settings, _) -> globalIndex in settings.indices }

                            if (settingsFlow != null) {
                                Timber.d("DOUBLE INITIAL RETRYFLOW encontrado: $globalIndex  campo: $dataTypeId ViewEmitter@$emitterId")
                                val (settings, generalSettings) = settingsFlow
                                emit(settings to generalSettings)
                            } else {
                                Timber.e("DOUBLE INITIAL index out of Bounds: $globalIndex ViewEmitter@$emitterId")
                                throw IndexOutOfBoundsException("Index out of Bounds ViewEmitter@$emitterId")
                            }
                        },
                        onFailure = { attempts, e ->
                            Timber.e("DOUBLE INITIAL index not valid in $attempts attemps. Error: $e ViewEmitter@$emitterId")
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
                                Timber.d("DOUBLE Waiting for initialization: $extension $globalIndex")
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
                            when (e) {
                                is CancellationException -> {
                                    Timber.d("DOUBLE Flow cancelled: $extension $globalIndex")
                                    throw e
                                }
                                else -> {
                                    Timber.e(e, "DOUBLE Flow error: $extension $globalIndex")
                                    throw e
                                }
                            }
                        }
                        .retryWhen { cause, attempt ->
                            when (cause) {
                                is CancellationException -> {
                                    Timber.d("DOUBLE Flow cancelled during retry: $extension $globalIndex ViewEmitter@$emitterId")
                                    false
                                }
                                else -> {
                                    if (attempt > 3) {
                                        Timber.e(cause, "DOUBLE Max retries reached: $extension $globalIndex (attempt $attempt) ViewEmitter@$emitterId")
                                        cleanupJobs()
                                        delay(Delay.RETRY_LONG.time)
                                        startView(context, config, emitter)
                                        false
                                    } else {
                                        Timber.w(cause, "DOUBLE Retrying flow: $extension $globalIndex (attempt $attempt) ViewEmitter@$emitterId")
                                        delay(Delay.RETRY_SHORT.time)
                                        true
                                    }
                                }
                            }
                        }
                        .launchIn(scope)
                } catch (e: CancellationException) {
                    Timber.d("DOUBLE View operation cancelled: $extension $globalIndex ViewEmitter@$emitterId")
                    throw e
                }
            } catch (e: CancellationException) {
                Timber.d("DOUBLE ViewJob cancelled: $extension $globalIndex ViewEmitter@$emitterId")
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE ViewJob error: $extension $globalIndex ViewEmitter@$emitterId")
                if (!scope.isActive) return@launch
                cleanupJobs()
                delay(1000)
                startView(context, config, emitter)
            }
        }

        emitter.setCancellable {
            try {
                Timber.d("DOUBLE Stopping view: $extension $globalIndex ViewEmitter@$emitterId")
                isInitialized.set(false)
                cleanupJobs()
            } catch (e: CancellationException) {
                Timber.d("DOUBLE Normal cancellation during cleanup: $extension $globalIndex ViewEmitter@$emitterId")
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Error during view cancellation: $extension $globalIndex ViewEmitter@$emitterId")
            }
        }
    }
}