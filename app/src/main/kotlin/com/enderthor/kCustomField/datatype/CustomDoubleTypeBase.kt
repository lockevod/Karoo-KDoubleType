package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.ColorFilter

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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*

import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamDoubleFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import timber.log.Timber


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

    companion object {
        private const val PREVIEW_DELAY = 2000L
        private const val RETRY_DELAY_SHORT = 2000L
        private const val RETRY_DELAY_LONG = 10000L
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
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }
         val job = scope.launch {

            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settingsFlow = if (config.preview) { if (index % 2 == 0) flowOf(previewDoubleVerticalFieldSettings)  else flowOf(previewDoubleHorizontalFieldSettings) }else context.streamDoubleFieldSettings().stateIn(scope, SharingStarted.WhileSubscribed(), listOf(DoubleFieldSettings()))
            val generalSettingsFlow = context.streamGeneralSettings().stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                .flatMapLatest { (settings, generalSettings) ->
                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                            createHeadwindFlow(karooSystem,refreshTime) else null

                    val firstFieldFlow = if (!config.preview) getFieldFlow(karooSystem, primaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()
                    val secondFieldFlow = if (!config.preview) getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSettings,refreshTime) else previewFlow()

                    combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                        Quadruple(generalSettings, settings, firstState, secondState)
                    }
                }.buffer()
                .map { (generalSettings, settings, firstFieldState, secondFieldState) ->
                    val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

                    val (firstvalue, firstIconcolor, firstColorzone) = getFieldState(firstFieldState, firstField(settings[index]), context, userProfile, generalSettings.ispalettezwift)
                    val (secondvalue, secondIconcolor, secondColorzone) = getFieldState(secondFieldState, secondField(settings[index]), context, userProfile, generalSettings.ispalettezwift)

                    val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                        val windData = (firstFieldState as? StreamHeadWindData) ?: (secondFieldState as StreamHeadWindData)
                        windData.diff to windData.windSpeed.roundToInt().toString()
                    } else 0.0 to ""

                    val fieldNumber = when {
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
                   // delay(if (karooSystem.hardwareType == HardwareType.K2) RefreshTime.MID.time else RefreshTime.HALF.time)
                    glance.compose(context, DpSize.Unspecified) {
                        DoubleScreenSelector(
                            fieldNumber,
                            ishorizontal(settings[index]),
                            firstvalue,
                            secondvalue,
                            firstField(settings[index]).kaction.icon,
                            secondField(settings[index]).kaction.icon,
                            ColorFilter.tint(firstIconcolor),
                            ColorFilter.tint(secondIconcolor),
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
                    }.remoteViews
                }
                .retryWhen { cause, attempt ->
                    Timber.e(cause, "Error collecting flow, retrying... (attempt $attempt)")
                    delay(if (attempt % 4 == 3L) RETRY_DELAY_LONG else RETRY_DELAY_SHORT)
                    true
                }
                .onEach { result ->
                    emitter.updateView(result)
                }
                .launchIn(scope)
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            job.cancel()
        }
    }
}