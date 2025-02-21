/*package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews

import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import timber.log.Timber
import kotlin.random.Random


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBaseOld2022(
    private val karooSystem: KarooSystemService,
    datatype: String,
    protected val index: Int
) : DataTypeImpl("kcustomfield", datatype) {
    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    companion object {
        private const val PREVIEW_DELAY = 2000L
        private const val RETRY_DELAY_SHORT = 2000L
        private const val RETRY_DELAY_LONG = 8000L
    }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time else RefreshTime.HALF.time


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
            delay(CustomDoubleTypeBase.PREVIEW_DELAY)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO)

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val viewjob = scope.launch {

            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settingsFlow = if (config.preview) { if (index % 2 == 0) flowOf(previewDoubleFieldSettings)  else flowOf(previewDoubleFieldSettings)}else context.streamDoubleFieldSettings().stateIn(scope, SharingStarted.WhileSubscribed(), listOf(DoubleFieldSettings()))
            val generalSettingsFlow = context.streamGeneralSettings().stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            var fieldNumber: Int =3
            var clayout: FieldPosition = FieldPosition.CENTER
            val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

            if (!config.preview) {
                try {
                    val initialRemoteViews = glance.compose(context, DpSize.Unspecified) {
                        NotSupported("Searching ...",21)
                    }.remoteViews
                    emitter.updateView(initialRemoteViews)

                    // Esperar 100, 200 o 300 ms antes de continuar con el resto
                    delay(100L + (Random.nextInt(2) * 100L))
                } catch (e: Exception) {
                    Timber.e(e, "DOUBLE Error en vista inicial: $extension ")
                }
                // Esperar a que termine la vista inicial
            }

            // Stream view

            combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                .flatMapLatest { (settings, generalSettings) ->
                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                            createHeadwindFlow(karooSystem,refreshTime) else flowOf(StreamHeadWindData(0.0, 0.0))

                    val firstFieldFlow = if (!config.preview) karooSystem.getFieldFlow(primaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()
                    val secondFieldFlow = if (!config.preview) karooSystem.getFieldFlow( secondaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()


                    combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                        Quadruple(generalSettings, settings, firstState, secondState)
                    }
                }
                .onEach { (generalSettings, settings, firstFieldState, secondFieldState) ->
                    // val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

                    val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(firstFieldState, firstField(settings[index]), context, userProfile, generalSettings.ispalettezwift)
                    val (secondvalue, secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldState(secondFieldState, secondField(settings[index]), context, userProfile, generalSettings.ispalettezwift)

                    val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                        val windData = (firstFieldState as? StreamHeadWindData) ?: (secondFieldState as StreamHeadWindData)
                        windData.diff to windData.windSpeed.roundToInt().toString()
                    } else 0.0 to ""

                    /* val fieldNumber = when {
                         firstFieldState is StreamState && secondFieldState is StreamState -> 3
                         firstFieldState is StreamState -> 0
                         secondFieldState is StreamState -> 1
                         else -> 2
                     }

                     val clayout = when {
                         fieldNumber != 3 -> FieldPosition.CENTER
                         generalSettings.iscenterkaroo -> when (config.alignment) {
                             ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                             ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                             ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                         }
                         ishorizontal(settings[index]) -> generalSettings.iscenteralign
                         else -> generalSettings.iscentervertical
                     }
                     */
                    // delay(if (karooSystem.hardwareType == HardwareType.K2) RefreshTime.MID.time else RefreshTime.HALF.time)

                    val result=glance.compose(context, DpSize.Unspecified) {
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

                    emitter.updateView(result)
                }
                .retryWhen { cause, attempt ->
                    if (attempt > 3) {
                        Timber.e(cause, "Error collecting Rolling flow, stopping.. (attempt $attempt) Cause: $cause")
                        scope.cancel()
                        //configJob.cancel()
                        //viewjob.cancel()
                        delay(RETRY_DELAY_LONG)
                        //startView(context, config, emitter)
                        true
                    } else {
                        Timber.e(cause, "Error collecting Rolling flow, retrying... (attempt $attempt) Cause: $cause")
                        delay(RETRY_DELAY_SHORT)
                        true
                    }
                }
                .launchIn(scope)
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            viewjob.cancel()
        }
    }
}*/