package com.enderthor.kCustomField.datatype
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.view.ViewTreeObserver
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

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

    private lateinit var viewjob: Job

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

    private fun previewFlow(): Pair<Flow<StreamState>,Long> = flow {
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
    }.flowOn(Dispatchers.IO) to 0L

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO)

        val index = if(config.preview) 0 else globalIndex


        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }



        Timber.d("Starting Double view field $extension and index $index and field $dataTypeId")
        viewjob = scope.launch {

            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settingsFlow = if (config.preview) {
                if (index % 2 == 0) flowOf(previewDoubleVerticalFieldSettings)
                else flowOf(previewDoubleHorizontalFieldSettings)
            }
            else context.streamDoubleFieldSettings().stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                listOf(DoubleFieldSettings())
            )

            val generalSettingsFlow = context.streamGeneralSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            var fieldNumber: Int =5
            var clayout: FieldPosition = FieldPosition.CENTER
            val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

            retryFlow(
                action = {
                    val settingsFlow = combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                        .firstOrNull { (settings, _) -> index in settings.indices }


                    if (settingsFlow != null) {
                        Timber.d("DOUBLE INITIAL RETRYFLOW encontrado: $index  campo: $dataTypeId" )
                        val (settings, generalSettings) = settingsFlow
                        emit(settings to generalSettings)
                    } else {
                        Timber.e("DOUBLE INITIAL index out of Bounds: $index campo: $dataTypeId")
                        throw IndexOutOfBoundsException("Index out of Bounds")
                    }
                },
                onFailure = { attempts, e ->
                    Timber.e("DOUBLE INITIAL index not valid in $attempts attemps. Error: $e")
                    emit( listOf(DoubleFieldSettings()) to GeneralSettings())
                }
            ).collectLatest { (settings, generalSettings) ->

                if (index in settings.indices)
                {
                    Timber.d("DOUBLE INITIAL index encontrado: $index datos: ${settings[index]} campo: $dataTypeId" )
                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

                    // Initial view

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
                                primaryField.kaction.icon,
                                secondaryField.kaction.icon,
                                ColorProvider(Color.Black, Color.White),
                                ColorProvider(Color.Black, Color.White),
                                ColorProvider(Color.White, Color.Black),
                                ColorProvider(Color.White, Color.Black),
                                getFieldSize(config.gridSize.second),
                                karooSystem.hardwareType == HardwareType.KAROO,
                                !(firstField(settings[index]).kaction.convert == "speed" || firstField(
                                    settings[index]
                                ).kaction.zone == "slopeZones" || firstField(settings[index]).kaction.label == "IF"),
                                !(secondField(settings[index]).kaction.convert == "speed" || secondField(
                                    settings[index]
                                ).kaction.zone == "slopeZones" || secondField(settings[index]).kaction.label == "IF"),
                                firstField(settings[index]).kaction.label,
                                secondField(settings[index]).kaction.label,
                                clayout,
                                "",
                                0,
                                baseBitmap,
                                false,
                                false
                            )
                        }.remoteViews
                        emitter.updateView(initialRemoteViews)

                    }
                } else Timber.d("DOUBLE INITIAL WITHOUT index fuera de los límites: $index,  campo: $dataTypeId")
            }


            // Stream view
            combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                .flatMapLatest { (settings, generalSettings) ->
                    Timber.d("DOUBLE FLAT index encontrado: $index datos: ${settings[index]} campo: $dataTypeId" )
                    val currentSetting = settings[index]
                   // Timber.d("currentSetting ok")

                    val primaryField = firstField(currentSetting)
                    val secondaryField = secondField(currentSetting)

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                            createHeadwindFlow(karooSystem,refreshTime) else flowOf(StreamHeadWindData(0.0, 0.0))

                   /*
                   val (firstFieldFlow,timefirstField) = if (!config.preview) getFieldFlow(karooSystem, primaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()
                    val (secondFieldFlow,timesecondField) = if (!config.preview) getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()

                    val resetfirst= if (firstFieldFlow is StreamState)  checkDataFlowTimeout(scope, primaryField.kaction.action, refreshTime, 60000, 10000, timefirstField) else false


                    if (resetfirst) {
                        firstFieldJob?.cancel()
                        firstFieldJob = scope.launch {
                            firstFieldFlow.collect { state ->
                                // No emitir, solo reiniciar el flujo
                            }
                        }
                        }
                        */
                    val firstFieldFlow =  getFieldFlow(karooSystem, primaryField, headwindFlow, generalSettings,refreshTime)
                    val secondFieldFlow =  getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSettings,refreshTime)


                    combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                        Quadruple(firstState, secondState,settings, generalSettings)
                    }
                }
                .debounce(refreshTime)
                .onEach { (firstFieldState, secondFieldState, settings, generalSettings) ->


                        val (firstvalue, firstIconcolor, firstColorzone) = getFieldState(
                            firstFieldState,
                            firstField(settings[index]),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )
                        val (secondvalue, secondIconcolor, secondColorzone) = getFieldState(
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


                        if(fieldNumber==5){
                           // Timber.d("determino el fieldlayout porque no he podido antes")
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
                        }

                        val result=glance.compose(context, DpSize.Unspecified) {
                            DoubleScreenSelector(
                                fieldNumber,
                                ishorizontal(settings[index]),
                                firstvalue,
                                secondvalue,
                                firstField(settings[index]).kaction.icon,
                                secondField(settings[index]).kaction.icon,
                                firstIconcolor,
                                secondIconcolor,
                                firstColorzone,
                                secondColorzone,
                                getFieldSize(config.gridSize.second),
                                karooSystem.hardwareType == HardwareType.KAROO,
                                !(firstField(settings[index]).kaction.convert == "speed" || firstField(settings[index]).kaction.zone == "slopeZones" || firstField(settings[index]).kaction.label == "IF"),
                                !(secondField(settings[index]).kaction.convert == "speed" || secondField(settings[index]).kaction.zone == "slopeZones" || secondField(settings[index]).kaction.label == "IF"),
                                firstField(settings[index]).kaction.label,
                                secondField(settings[index]).kaction.label,
                                clayout,
                                windtext,
                                winddiff.roundToInt(),
                                baseBitmap,
                                firstField(settings[index]).iszone,
                                secondField(settings[index]).iszone
                            )
                        }.remoteViews
                        //Timber.d("DOUBLE RESULT $result campo: $dataTypeId")

                        emitter.updateView(result)



                   // } else
                     //   Timber.e("DOUBLE VIEW index fuera de los límites: $index, Tamaño: ${settings.size} campo: $extension")
                }
                .retryWhen { cause, attempt ->
                    if (attempt > 4) {
                        Timber.e(cause, "Error collecting Double flow, stopping.. (attempt $attempt) Cause: $cause")
                        scope.cancel()
                        configJob.cancel()
                        viewjob.cancel()
                        delay(Delay.RETRY_LONG.time)
                        startView(context, config, emitter)
                        false
                    }
                    else {
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
        }
    }
}