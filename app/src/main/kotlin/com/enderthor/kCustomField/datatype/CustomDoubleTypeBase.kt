package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamDoubleFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import com.enderthor.kCustomField.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val index: Int
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal = { settings: DoubleFieldSettings -> settings.ishorizontal }

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(
                StreamState.Streaming(
                    DataPoint(
                        dataTypeId,
                        mapOf(DataType.Field.SINGLE to 1.0),
                        extension
                    )
                )
            )
        }
        emitter.setCancellable {
            Timber.d("stop speed stream")
            job.cancel()
        }
    }

    @OptIn(FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        data class StreamHeadWindData(val diff: Double, val windSpeed: Double)

        val job = CoroutineScope(Dispatchers.IO).launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            context.streamDoubleFieldSettings()
                .combine(context.streamGeneralSettings()) { settings, generalSettings ->
                    settings to generalSettings
                }
                .collect { (settings, generalSettings) ->
                    val firstFieldAction = firstField(settings[index]).kaction.action
                    val secondFieldAction = secondField(settings[index]).kaction.action

                    val headwindFlow = if (firstField(settings[index]).kaction.name == "HEADWIND" || secondField(settings[index]).kaction.name == "HEADWIND") {
                        karooSystem.streamDataFlow(Headwind.DIFF.type)
                            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                            .combine(karooSystem.streamDataFlow(Headwind.SPEED.type)
                                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }) { headwindDiff, headwindSpeed ->
                                StreamHeadWindData(headwindDiff, headwindSpeed)
                            }
                            .onStart { emit(StreamHeadWindData(0.0, 0.0)) }
                            .distinctUntilChanged() // Evitar valores repetidos consecutivamente
                            .debounce(100) // Procesar valores después de un período de inactividad
                            .buffer(capacity = Channel.BUFFERED) // Añadir buffer para evitar bloqueos
                            .catch { e ->
                                Timber.e(e, "Error en headwindFlow")
                                emit(StreamHeadWindData(0.0, 0.0)) // Emitir un valor por defecto en caso de error
                            }
                            .stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily, StreamHeadWindData(0.0, 0.0)) // Compartir el flujo
                    } else {
                        null
                    }

                    val firstFieldFlow = if (firstField(settings[index]).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled) {
                        headwindFlow ?: karooSystem.streamDataFlow(firstFieldAction)
                    } else {
                        karooSystem.streamDataFlow(firstFieldAction)
                    }

                    val secondFieldFlow = if (secondField(settings[index]).kaction.name == "HEADWIND" && generalSettings.isheadwindenabled) {
                        headwindFlow ?: karooSystem.streamDataFlow(secondFieldAction)
                    } else {
                        karooSystem.streamDataFlow(secondFieldAction)
                    }

                    firstFieldFlow.combine(secondFieldFlow) { firstState, secondState ->
                        Quadruple(generalSettings, settings, firstState, secondState)
                    }.collect { (generalSettings, settings, firstFieldState, secondFieldState) ->

                        fun updateFieldState(fieldState: StreamState, fieldSettings: DoubleFieldType): Triple<Double, ColorFilter, ColorProvider> {
                            val value = convertValue(fieldState, fieldSettings.kaction.convert, userProfile.preferredUnit.distance, fieldSettings.kaction.action)
                            val iconColor = getColorFilter(context, fieldSettings.kaction, fieldSettings.iszone)
                            val colorZone = getColorZone(context, fieldSettings.kaction.zone, value, userProfile, generalSettings.ispalettezwift).takeIf {
                                (fieldSettings.kaction.zone == "heartRateZones" || fieldSettings.kaction.zone == "powerZones" || fieldSettings.kaction.zone == "slopeZones") && fieldSettings.iszone
                            } ?: ColorProvider(Color.White, Color.Black)
                            return Triple(value, iconColor, colorZone)
                        }

                        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

                        val (firstvalue, firstIconcolor, firstColorzone) = if (firstFieldState is StreamState) {
                            updateFieldState(firstFieldState, firstField(settings[index]))
                        } else Triple(0.0, ColorFilter.tint(ColorProvider(Color.White, Color.Black)), ColorProvider(Color.White, Color.Black))

                        val (secondvalue, secondIconcolor, secondColorzone) = if (secondFieldState is StreamState) {
                            updateFieldState(secondFieldState, secondField(settings[index]))
                        } else Triple(0.0, ColorFilter.tint(ColorProvider(Color.White, Color.Black)), ColorProvider(Color.White, Color.Black))

                        val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                            val windData = (firstFieldState as? StreamHeadWindData) ?: (secondFieldState as StreamHeadWindData)
                            windData.diff to windData.windSpeed.roundToInt().toString()
                        } else 0.0 to ""

                        val clayout = if (generalSettings.iscenterkaroo) {
                            when (config.alignment) {
                                ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                            }
                        } else {
                            if (ishorizontal(settings[index])) generalSettings.iscenteralign else generalSettings.iscentervertical
                        }

                        val fieldNumber = when {
                            firstFieldState is StreamState && secondFieldState is StreamState -> 3
                            firstFieldState is StreamState -> 0
                            secondFieldState is StreamState -> 1
                            else -> 2
                        }

                        val result = glance.compose(context, DpSize.Unspecified) {
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
                                baseBitmap
                            )
                        }

                        emitter.updateView(result.remoteViews)
                    }
                }
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            job.cancel()
        }
    }
}