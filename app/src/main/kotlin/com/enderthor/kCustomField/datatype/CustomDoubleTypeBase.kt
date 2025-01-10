package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
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
import timber.log.Timber

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()
    protected val firstField = { settings: DoubleFieldSettings -> settings.onefield }
    protected val secondField = { settings: DoubleFieldSettings -> settings.secondfield }
    protected val ishorizontal= { settings: DoubleFieldSettings -> settings.ishorizontal}
    //protected val isenabled = { settings: DoubleFieldSettings -> settings.isenabled }

    //abstract val firstField: (DoubleFieldSettings) -> DoubleFieldType
    //abstract val secondField: (DoubleFieldSettings) -> DoubleFieldType
    //abstract val showh: (DoubleFieldSettings) -> Boolean
    abstract val index: Int
    //abstract val isenabled: Boolean

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

override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }
        Timber.d("Starting double view with $emitter")
        val job = CoroutineScope(Dispatchers.IO).launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            context.streamDoubleFieldSettings()
                .combine(context.streamGeneralSettings()) { settings, generalSettings ->
                    settings to generalSettings
                }
                .collect { (settings, generalSettings) ->
                    karooSystem.streamDataFlow(firstField(settings[index]).kaction.action)
                        .combine(karooSystem.streamDataFlow(secondField(settings[index]).kaction.action)) { firstState, secondState ->
                            Quadruple(
                                generalSettings,
                                settings,
                                firstState,
                                secondState
                            )
                        }
                        .collect { (generalSettings, settings, firstFieldState, secondFieldState) ->

                        Timber.d("Firstfield state: $firstFieldState")
                        val firstvalue = convertValue(
                            firstFieldState,
                            firstField(settings[index]).kaction.convert,
                            userProfile.preferredUnit.distance,
                            firstField(settings[index]).kaction.action
                        )
                        val secondvalue = convertValue(
                            secondFieldState,
                            secondField(settings[index]).kaction.convert,
                            userProfile.preferredUnit.distance,
                            secondField(settings[index]).kaction.action
                        )
                        val firstIconcolor = getColorFilter(
                            context,
                            firstField(settings[index]).kaction,
                            firstField(settings[index]).iszone
                        )
                        val secondIconcolor = getColorFilter(
                            context,
                            secondField(settings[index]).kaction,
                            secondField(settings[index]).iszone
                        )

                        val firstColorzone = getColorZone(
                            context,
                            firstField(settings[index]).kaction.zone,
                            firstvalue,
                            userProfile,
                            generalSettings.ispalettezwift
                        ).takeIf {
                            (firstField(settings[index]).kaction.zone == "heartRateZones" || firstField(
                                settings[index]
                            ).kaction.zone == "powerZones" || firstField(settings[index]).kaction.zone == "slopeZones") && firstField(
                                settings[index]
                            ).iszone
                        } ?: ColorProvider(Color.White, Color.Black)

                        val secondColorzone = getColorZone(
                            context,
                            secondField(settings[index]).kaction.zone,
                            secondvalue,
                            userProfile,
                            generalSettings.ispalettezwift
                        ).takeIf {
                            (secondField(settings[index]).kaction.zone == "heartRateZones" || secondField(
                                settings[index]
                            ).kaction.zone == "powerZones" || secondField(settings[index]).kaction.zone == "slopeZones") && secondField(
                                settings[index]
                            ).iszone
                        } ?: ColorProvider(Color.White, Color.Black)

                        Timber.d("Index: $index first value: $firstvalue AND second value: $secondvalue AND first action is ${firstField(settings[index]).kaction.action} AND second action is ${secondField(settings[index]).kaction.action}")
                        val result = glance.compose(context, DpSize.Unspecified) {
                            DoubleScreenSelector(
                                ishorizontal(settings[index]),
                                firstvalue,
                                secondvalue,
                                firstField(settings[index]).kaction.icon,
                                secondField(settings[index]).kaction.icon,
                                firstIconcolor,
                                secondIconcolor,
                                true,
                                firstColorzone,
                                secondColorzone,
                                getFieldSize(config.gridSize.second),
                                karooSystem.hardwareType == HardwareType.KAROO,
                                !(firstField(settings[index]).kaction.convert == "speed" || firstField(settings[index]).kaction.zone == "slopeZones" || firstField(settings[index]).kaction.label == "IF"),
                                !(secondField(settings[index]).kaction.convert == "speed" || secondField(settings[index]).kaction.zone == "slopeZones" || secondField(settings[index]).kaction.label == "IF"),
                                firstField(settings[index]).kaction.label,
                                secondField(settings[index]).kaction.label,
                                if (ishorizontal(settings[index])) generalSettings.iscenteralign else generalSettings.iscentervertical
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