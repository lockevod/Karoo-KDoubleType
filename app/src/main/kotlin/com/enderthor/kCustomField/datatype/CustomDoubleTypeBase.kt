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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*

import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamDoubleFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.streamOneFieldSettings

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

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
           // context.streamGeneralSettings().collect {
            emitter.onNext(
                StreamState.Streaming(
                    DataPoint(
                        dataTypeId,
                        mapOf(DataType.Field.SINGLE to 1.0),
                        extension
                    )
                )
            )
                delay(if (karooSystem.hardwareType == HardwareType.K2) RefreshTime.MID.time else RefreshTime.HALF.time)

           // }
        }
        emitter.setCancellable {
            Timber.d("stop speed stream")
            job.cancel()
        }
    }
/*
    @OptIn(FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val job = CoroutineScope(Dispatchers.IO).launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            combine(context.streamDoubleFieldSettings(), context.streamGeneralSettings()) { settings, generalSettings -> settings to generalSettings }
                .collect { (settings, generalSettings) ->

                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                            createHeadwindFlow(karooSystem) else null

                    val firstFieldFlow = getFieldFlow(karooSystem,primaryField, headwindFlow, generalSettings)
                    val secondFieldFlow = getFieldFlow(karooSystem,secondaryField, headwindFlow, generalSettings)

                    combine( firstFieldFlow,secondFieldFlow) { firstState, secondState ->
                        Quadruple(generalSettings, settings, firstState, secondState)
                    }.collect { (generalSettings, settings, firstFieldState, secondFieldState) ->

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

                        //Timber.d("config = $config")
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

 */

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO)

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val job = scope.launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()

            val settingsFlow = context.streamDoubleFieldSettings().stateIn(scope, SharingStarted.Lazily, listOf(DoubleFieldSettings()))
            val generalSettingsFlow = context.streamGeneralSettings().stateIn(scope, SharingStarted.Lazily, GeneralSettings())

            combine(settingsFlow, generalSettingsFlow) { settings, generalSettings -> settings to generalSettings }
                .flatMapLatest { (settings, generalSettings) ->
                    val primaryField = firstField(settings[index])
                    val secondaryField = secondField(settings[index])

                    val headwindFlow =
                        if (listOf(primaryField, secondaryField).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled)
                            createHeadwindFlow(karooSystem) else null

                    val firstFieldFlow = getFieldFlow(karooSystem, primaryField, headwindFlow, generalSettings)
                    val secondFieldFlow = getFieldFlow(karooSystem, secondaryField, headwindFlow, generalSettings)

                    combine(firstFieldFlow, secondFieldFlow) { firstState, secondState ->
                        Quadruple(generalSettings, settings, firstState, secondState)
                    }
                }
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
                    delay(1000)
                    true
                }
                .collect { result ->
                    emitter.updateView(result)
                }
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            job.cancel()
        }
    }
}