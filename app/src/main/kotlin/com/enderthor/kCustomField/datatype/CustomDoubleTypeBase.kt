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
import com.enderthor.kCustomField.datatype.previewDoubleFieldSettings
import com.enderthor.kCustomField.extensions.KarooCustomFieldExtension
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import com.enderthor.kCustomField.extensions.streamUserProfile
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import com.enderthor.kCustomField.datatype.previewDoubleFieldSettings

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    protected val karooSystem: KarooSystemService,
    protected val karooExtension: KarooCustomFieldExtension,
    datatype: String,
    protected val globalIndex: Int
) : DataTypeImpl(karooExtension.extensionId, datatype) {


    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    private val refreshTime: Long
        get() = when (karooSystem.hardwareType) {
            HardwareType.K2 -> RefreshTime.MID.time
            else -> RefreshTime.HALF.time
        }.coerceAtLeast(100L) // Aseguramos un mínim

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
                   delay(refreshTime)
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
        val scope = CoroutineScope(Dispatchers.IO + Job())


        val dataflow = context.streamDoubleFieldSettings()
            .onStart {
                Timber.d("Iniciando streamDoubleFieldSettings")
                emit(previewDoubleFieldSettings as MutableList<DoubleFieldSettings>)
            }
            .combine(
                context.streamGeneralSettings()
                    .onStart {
                        Timber.d("Iniciando streamGeneralSettings")
                        emit(GeneralSettings())
                    }
            ) { settings, generalSettings ->
                settings to generalSettings
            }.combine(
                karooSystem.streamUserProfile()

            ) { (settings, generalSettings), userProfile ->
                GlobalConfigState(settings, generalSettings, userProfile)
            }



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
               // val (userProfile, settingsFlow, generalSettingsFlow) = initializeView(scope, context)
                //Esperar entre 10 ms y 200 ms antes de empezar
                //delay(15L + (Random.nextInt(3) * 15L))
                try {
                    // Carga inicial rápida
                    if (!config.preview) {
                            try {
                                val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                                 NotSupported("Searching ...",21)
                                }.remoteViews
                                emitter.updateView(initialRemoteViews)

                                // Esperar 100, 200 o 300 ms antes de continuar con el resto
                                delay(100L + (Random.nextInt(2) * 100L))
                            } catch (e: Exception) {
                                Timber.e(e, "DOUBLE Error en vista inicial: $extension $globalIndex ViewEmitter@$emitterId")
                            }
                       // Esperar a que termine la vista inicial
                    }

                    Timber.d("DOUBLE Starting view flow: $extension $globalIndex ViewEmitter@$emitterId karooSystem@$karooSystem ")


                    dataflow.flatMapLatest { state ->
                            val (settings, generalSettings, userProfile) = state

                            if (userProfile == null) {
                                Timber.d("DOUBLE UserProfile no disponible")
                                return@flatMapLatest flowOf(Triple(
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    state
                                ))
                            }

                            val currentSettings = settings.getOrNull(globalIndex)
                                ?: throw IndexOutOfBoundsException("Invalid index $globalIndex")

                            val primaryField = firstField(currentSettings)
                            val secondaryField = secondField(currentSettings)

                            val headwindFlow =
                                if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                                    createHeadwindFlow(karooSystem, refreshTime) else flowOf(StreamHeadWindData(0.0, 0.0))

                            val firstFieldFlow = if (!config.preview) karooSystem.getFieldFlow(primaryField, headwindFlow, generalSettings) else previewFlow()
                            val secondFieldFlow = if (!config.preview) karooSystem.getFieldFlow(secondaryField, headwindFlow, generalSettings) else previewFlow()

                            combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                                Triple(firstState, secondState, state)
                            }
                        }
                        .onEach { (firstFieldState, secondFieldState, globalConfig) ->

                            val (setting, generalSettings, userProfile) = globalConfig

                            if (userProfile == null) {
                                Timber.d("UserProfile no disponible")
                                return@onEach
                            }
                            val settings = setting[globalIndex]

                            val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(
                                firstFieldState,
                                firstField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (secondvalue, secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldState(
                                secondFieldState,
                                secondField((settings)),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                                val windData = (firstFieldState as? StreamHeadWindData)
                                    ?: (secondFieldState as StreamHeadWindData)
                                windData.diff to windData.windSpeed.roundToInt().toString()
                            } else 0.0 to ""

                            val fieldNumber = when {
                                firstFieldState is StreamState && secondFieldState is StreamState -> 3
                                firstFieldState is StreamState -> 0
                                secondFieldState is StreamState -> 1
                                else -> 2
                            }

                            val clayout = when {
                                //fieldNumber != 3 -> FieldPosition.CENTER
                                generalSettings.iscenterkaroo -> when (config.alignment) {
                                    ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                    ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                    ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                                }
                                ishorizontal(settings) -> generalSettings.iscenteralign
                                else -> generalSettings.iscentervertical
                            }

                            try {
                                val newView = glance.compose(context, DpSize.Unspecified) {
                                    DoubleScreenSelector(
                                        fieldNumber,
                                        ishorizontal(settings),
                                        firstvalue,
                                        secondvalue,
                                        firstField(settings),
                                        secondField(settings),
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
                cleanupJobs()
            } catch (e: CancellationException) {
                Timber.d("DOUBLE Normal cancellation during cleanup: $extension $globalIndex ViewEmitter@$emitterId")
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Error during view cancellation: $extension $globalIndex ViewEmitter@$emitterId")
            }
        }
    }
}