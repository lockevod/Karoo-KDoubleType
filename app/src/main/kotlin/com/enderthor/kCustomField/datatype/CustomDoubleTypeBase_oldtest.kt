package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import android.widget.RemoteViews
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import com.enderthor.kCustomField.datatype.previewDoubleFieldSettings
import com.enderthor.kCustomField.extensions.streamDataFlow
import io.hammerhead.karooext.models.DataType.Field
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase_oldtest(
    protected val karooSystem: KarooSystemService,
    protected val applicationContext: Context,
    protected val datatype: String,
    protected val globalIndex: Int
) : DataTypeImpl("kcustomfield", datatype) {


    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    private val refreshTime: Long
        get() = when (karooSystem.hardwareType) {
            HardwareType.K2 -> RefreshTime.MID.time
            else -> RefreshTime.HALF.time
        }.coerceAtLeast(100L) // Aseguramos un mínim



    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("DOUBLE Starting stream: $extension $globalIndex")
        val job = CoroutineScope(Dispatchers.IO).launch {
            applicationContext.streamDoubleFieldSettings()
                .combine(applicationContext.streamGeneralSettings()) { settings, generalSettings ->
                    settings to generalSettings
                }
                .collect { (settings, generalSettings) ->
                    val mValues = mutableMapOf<String, Double>()
                    var isStreaming = false

                    val primaryField = firstField(settings[globalIndex])
                    val secondaryField = secondField(settings[globalIndex])

                    val headwindFlow = if (listOf(primaryField, secondaryField)
                            .any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled
                    ) {
                        createHeadwindFlow(karooSystem, refreshTime)
                    } else {
                        flowOf(StreamHeadWindData(0.0, 0.0))
                    }

                    val firstFieldFlow =
                        karooSystem.getFieldFlow(primaryField, headwindFlow, generalSettings)
                    val secondFieldFlow =
                        karooSystem.getFieldFlow(secondaryField, headwindFlow, generalSettings)



                    combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                        Pair(firstState, secondState)
                    }
                        .collect { (firstState, secondState) ->
                            // Procesar primer campo
                            if (firstState is StreamState) {
                                val value =
                                    convertValueStart(firstState, primaryField.kaction.action)
                                mValues.put("PRIMARY", value)
                                mValues.put("PRIMARY_DIFF", 0.0)
                                isStreaming = true
                            } else if (firstState is StreamHeadWindData) {
                                mValues.put("PRIMARY", firstState.windSpeed)
                                mValues.put("PRIMARY_DIFF", firstState.diff)
                                isStreaming = true
                            }

                            // Procesar segundo campo
                            if (secondState is StreamState) {
                                val value =
                                    convertValueStart(secondState, secondaryField.kaction.action)
                                mValues.put("SECONDARY", value)
                                mValues.put("SECONDARY_DIFF", 0.0)
                                isStreaming = true
                            } else if (secondState is StreamHeadWindData) {
                                mValues.put("SECONDARY", secondState.windSpeed)
                                mValues.put("SECONDARY_DIFF", secondState.diff)
                                isStreaming = true
                            }

                            val valStreamState = if (isStreaming && mValues.isNotEmpty()) {
                                StreamState.Streaming(DataPoint(dataTypeId, mValues))
                            } else {
                                StreamState.Searching
                            }
                            Timber.d("DOUBLE STARTSTREM Using dataTypeId: $dataTypeId extension: $extension")
                            Timber.d("DOUBLE Emitting stream: $extension $globalIndex $valStreamState")
                            emitter.onNext(valStreamState)
                            delay(refreshTime)
                        }
                }
        }
        emitter.setCancellable {
            Timber.d("DOUBLE Stopping stream: $extension $globalIndex ")
            job.cancel()
        }
    }


    private fun previewFlow(): Flow<StreamState> = flow {
        while (true) {
            emit(
                StreamState.Streaming(
                    DataPoint(
                        dataTypeId,
                        mapOf(Field.SINGLE to (0..100).random().toDouble()),
                        extension
                    )
                )
            )
            delay(Delay.PREVIEW.time)
        }
    }.flowOn(Dispatchers.IO)
    /*
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("DOUBLE StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")

        val scope = CoroutineScope(Dispatchers.IO)
        var isStreaming = false


        val settingsFlow = context.streamDoubleFieldSettings()
            .combine(
                context.streamGeneralSettings()
            ) { settings, generalSettings ->
                settings to generalSettings
            }.combine(
                karooSystem.streamUserProfile()
            ) { (settings, generalSettings), userProfile ->
                Timber.d("DOUBLE Flow dataflow UserProfile: $userProfile settings: $settings generalSettings: $generalSettings")
                GlobalConfigState(settings, generalSettings, userProfile)
            }

        val streamFlow = if (config.preview) {
            flowOf(Quadruple((0..100).random().toDouble(), 0.0, (0..100).random().toDouble(), 0.0))
        } else {
            karooSystem.streamDataFlow(dataTypeId)
                .combine(karooSystem.streamDataFlow(dataTypeId)) { firstStream, secondStream ->
                    var first1 = 0.0
                    var first2 = 0.0
                    var second1 = 0.0
                    var second2 = 0.0

                    if (firstStream is StreamState.Streaming) {
                        isStreaming = true
                        first1 = firstStream.dataPoint.values["PRIMARY"] ?: 0.0
                        first2 = firstStream.dataPoint.values["PRIMARY_DIFF"] ?: 0.0
                    } else {
                        isStreaming = false
                    }

                    if (secondStream is StreamState.Streaming) {
                        isStreaming = true
                        second1 = secondStream.dataPoint.values["SECONDARY"] ?: 0.0
                        second2 = secondStream.dataPoint.values["SECONDARY_DIFF"] ?: 0.0
                    }

                    Quadruple(first1, first2, second1, second2)
                }
        }


        val configJob = scope.launch {
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
                Timber.e(e, "DOUBLE Error in config job: $extension $globalIndex ")
            }
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

        val viewjob = scope.launch {
            try {
                Timber.d("DOUBLE Starting view: $extension $globalIndex ")

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
                                Timber.e(e, "DOUBLE Error en vista inicial: $extension $globalIndex ")
                            }
                       // Esperar a que termine la vista inicial
                    }

                    Timber.d("DOUBLE Starting view flow: $extension $globalIndex  karooSystem@$karooSystem ")


                    settingsFlow.flatMapLatest { state ->


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

                           /* val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(
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
*/
                            val firstFieldisHW=firstField(settings).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled
                            val secondFieldisHW=secondField(settings).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled

                            val (firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldStateView(
                                firstField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift,
                            )

                            val ( secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldStateView(
                                secondField((settings)),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift,
                            )



                            va
                            val (winddiff, windtext) = when {
                                firstFieldisHW -> firstFieldState.diff to firstFieldState.windSpeed.roundToInt().toString()
                                secondFieldisHW -> secondFieldState.diff to secondFieldState.windSpeed.roundToInt().toString()
                                else -> 0.0 to ""
                            }

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
                                }
                                else -> {
                                    Timber.e(e, "DOUBLE Flow error: $extension $globalIndex")

                                }
                            }
                        }
                        .retryWhen { cause, attempt ->
                                    if (attempt > 3) {
                                        Timber.e(cause, "DOUBLE Max retries reached: $extension $globalIndex (attempt $attempt) ")
                                        delay(Delay.RETRY_LONG.time)
                                        true
                                    } else {
                                        Timber.w(cause, "DOUBLE Retrying flow: $extension $globalIndex (attempt $attempt) ")
                                        delay(Delay.RETRY_SHORT.time)
                                        true
                                    }
                            }
                        .launchIn(scope)


            } catch (e: Exception) {
                Timber.e(e, "DOUBLE ViewJob error: $extension $globalIndex ")
                //if (!scope.isActive) return@launch
                delay(10000L)
                startView(context, config, emitter)
            }
        }

        emitter.setCancellable {
            try {
                Timber.d("DOUBLE Stopping view: $extension $globalIndex ")
                viewjob.cancel()
                configJob.cancel()

            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Error during view cancellation: $extension $globalIndex ")
            }
        }
    }*/

    data class ViewData(
        val fieldNumber: Int,
        val isHorizontal: Boolean,
        val firstValue: Double,
        val secondValue: Double,
        val firstField: DoubleFieldType,
        val secondField: DoubleFieldType,
        val firstIconColor: ColorProvider,
        val secondIconColor: ColorProvider,
        val firstColorZone: ColorProvider,
        val secondColorZone: ColorProvider,
        val fieldSize: FieldSize,
        val isKaroo: Boolean,
        val layout: FieldPosition,
        val windText: String,
        val windDiff: Int,
        val baseBitmap: Bitmap,
        val isDivider: Boolean,
        val firstValueRight: Double,
        val secondValueRight: Double
    )


    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("DOUBLE StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")

        val scope = CoroutineScope(Dispatchers.IO)
        var isStreaming = false

        val settingsFlow = context.streamDoubleFieldSettings()
            .combine(context.streamGeneralSettings()) { settings, generalSettings ->
                Timber.d("DOUBLE Flow dataflow settings: $settings generalSettings: $generalSettings")
                settings to generalSettings
            }.combine(karooSystem.streamUserProfile()) { (settings, generalSettings), userProfile ->
                Timber.d("DOUBLE Flow dataflow UserProfile: $userProfile settings: $settings generalSettings: $generalSettings")
                Triple(settings, generalSettings, userProfile)
            }
        Timber.d("DOUBLE Flow Datatype : $dataTypeId")
        Timber.d("DOUBLE Using dataTypeId: $dataTypeId extension: $extension")
        val streamFlow = if (config.preview) {
            flowOf(Quadruple((0..100).random().toDouble(), 0.0, (0..100).random().toDouble(), 0.0))
        } else {
            karooSystem.streamDataFlow(dataTypeId)
                .onStart {
                    Timber.d("DOUBLE Flow starting with dataTypeId: $dataTypeId")
                }
                .map{ stream ->
                    Timber.d("DOUBLE Flow received stream: $stream")
                    var first1 = 0.0
                    var first2 = 0.0
                    var second1 = 0.0
                    var second2 = 0.0

                    when (stream) {
                        is StreamState.Streaming -> {
                            isStreaming = true
                            first1 = stream.dataPoint.values["PRIMARY"] ?: 0.0
                            first2 = stream.dataPoint.values["PRIMARY_DIFF"] ?: 0.0
                            second1 = stream.dataPoint.values["SECONDARY"] ?: 0.0
                            second2 = stream.dataPoint.values["SECONDARY_DIFF"] ?: 0.0
                            Timber.d("DOUBLE Flow streaming with values: PRIMARY=${first1}, PRIMARY_DIFF=${first2}, SECONDARY=${second1}, SECONDARY_DIFF=${second2}")
                        }
                        is StreamState.Searching -> {
                            isStreaming = false
                            Timber.d("DOUBLE Flow in searching state")
                        }
                        else -> {
                            isStreaming = false
                            Timber.d("DOUBLE Flow in unknown state: ${stream::class.simpleName}")
                        }
                    }
                    Quadruple(first1, first2, second1, second2)
                }
        }

        val configJob = scope.launch {
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
                Timber.e(e, "DOUBLE Error in config job: $extension $globalIndex ")
            }
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

        val viewjob = scope.launch {
            try {
                Timber.d("DOUBLE Starting viewjob: $extension $globalIndex ")

                if (!config.preview) {
                    try {
                        val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                            NotSupported("Searching ...", 21)
                        }.remoteViews
                        emitter.updateView(initialRemoteViews)

                    } catch (e: Exception) {
                        Timber.e(e, "DOUBLE Error en vista inicial: $extension $globalIndex ")
                    }
                }

                Timber.d("DOUBLE Starting view flow 2: $extension $globalIndex karooSystem@$karooSystem ")

                delay(100L + (Random.nextInt(5) * 100L))

                settingsFlow.combine(streamFlow) { configState, values ->
                    val (currentsettings, generalSettings, userProfile) = configState
                    val (firstValue, firstDiff, secondValue, secondDiff) = values

                    if (false) {
                        Timber.d("UserProfile no disponible")
                        return@combine null
                    }
                    Timber.d("DOUBLE Flow dataflow values DENTRO COMBINE: $firstValue, $firstDiff, $secondValue, $secondDiff")
                    val settings = currentsettings[globalIndex]
                    val firstFieldisHW =
                        firstField(settings).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled
                    val secondFieldisHW =
                        secondField(settings).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled

                    val (firstIconcolor, firstColorzone, isleftzone) = getFieldStateView(
                        firstField(settings),
                        context,
                        userProfile,
                        generalSettings.ispalettezwift,
                        firstFieldisHW,
                        firstValue
                    )

                    val (secondIconcolor, secondColorzone, isrightzone) = getFieldStateView(
                        secondField(settings),
                        context,
                        userProfile,
                        generalSettings.ispalettezwift,
                        secondFieldisHW,
                        secondValue
                    )

                    val (winddiff, windtext) = when {
                        firstFieldisHW -> firstDiff to firstValue.roundToInt().toString()
                        secondFieldisHW -> secondDiff to secondValue.roundToInt().toString()
                        else -> 0.0 to ""
                    }

                    val fieldNumber = when {
                        firstValue > 0 && secondValue > 0 -> 3
                        firstValue > 0 -> 0
                        secondValue > 0 -> 1
                        else -> 2
                    }

                    val clayout = when {
                        generalSettings.iscenterkaroo -> when (config.alignment) {
                            ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                            ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                            ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                        }

                        ishorizontal(settings) -> generalSettings.iscenteralign
                        else -> generalSettings.iscentervertical
                    }

                    ViewData(
                        fieldNumber = fieldNumber,
                        isHorizontal = ishorizontal(settings),
                        firstValue = firstValue,
                        secondValue = secondValue,
                        firstField = firstField(settings),
                        secondField = secondField(settings),
                        firstIconColor = firstIconcolor,
                        secondIconColor = secondIconcolor,
                        firstColorZone = firstColorzone,
                        secondColorZone = secondColorzone,
                        fieldSize = getFieldSize(config.gridSize.second),
                        isKaroo = karooSystem.hardwareType == HardwareType.KAROO,
                        layout = clayout,
                        windText = windtext,
                        windDiff = winddiff.roundToInt(),
                        baseBitmap = baseBitmap,
                        isDivider = generalSettings.isdivider,
                        firstValueRight = firstDiff,
                        secondValueRight = secondDiff
                    )
                }
                    .filterNotNull()
                    .onEach { viewData ->
                        try {
                            val newView = glance.compose(context, DpSize.Unspecified) {
                                DoubleScreenSelector(
                                    viewData.fieldNumber,
                                    viewData.isHorizontal,
                                    viewData.firstValue,
                                    viewData.secondValue,
                                    viewData.firstField,
                                    viewData.secondField,
                                    viewData.firstIconColor,
                                    viewData.secondIconColor,
                                    viewData.firstColorZone,
                                    viewData.secondColorZone,
                                    viewData.fieldSize,
                                    viewData.isKaroo,
                                    viewData.layout,
                                    viewData.windText,
                                    viewData.windDiff,
                                    viewData.baseBitmap,
                                    viewData.isDivider,
                                    viewData.firstValueRight,
                                    viewData.secondValueRight
                                )
                            }.remoteViews

                            emitter.updateView(newView)
                        } catch (e: Exception) {
                            Timber.e(
                                e,
                                "DOUBLE Error composing/updating view: $extension $globalIndex"
                            )
                        }
                    }
                    .retryWhen { cause, attempt ->
                        if (attempt > 3) {
                            Timber.e(
                                cause,
                                "DOUBLE Max retries reached: $extension $globalIndex (attempt $attempt) "
                            )
                            delay(Delay.RETRY_LONG.time)
                            true
                        } else {
                            Timber.w(
                                cause,
                                "DOUBLE Retrying flow: $extension $globalIndex (attempt $attempt) "
                            )
                            delay(Delay.RETRY_SHORT.time)
                            true
                        }
                    }
                    .launchIn(scope)

            } catch (e: Exception) {
                Timber.e(e, "DOUBLE ViewJob error: $extension $globalIndex ")
                delay(10000L)
                startView(context, config, emitter)
            }
        }

        emitter.setCancellable {
            try {
                Timber.d("DOUBLE Stopping view: $extension $globalIndex ")
                viewjob.cancel()
                configJob.cancel()
            } catch (e: Exception) {
                Timber.e(e, "DOUBLE Error during view cancellation: $extension $globalIndex ")
            }
        }
    }
}
