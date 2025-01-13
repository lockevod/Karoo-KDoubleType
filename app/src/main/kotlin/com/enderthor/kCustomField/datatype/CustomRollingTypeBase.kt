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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomRollingTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val index: Int
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()

    protected val firstField= { settings: OneFieldSettings -> settings.onefield }
    protected val secondField= { settings: OneFieldSettings -> settings.secondfield }
    protected val thirdField= { settings: OneFieldSettings -> settings.thirdfield }
    protected val time= { settings: OneFieldSettings -> settings.rollingtime }


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

        val job = CoroutineScope(Dispatchers.IO).launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            context.streamOneFieldSettings()
                .combine(context.streamGeneralSettings()) { settings, generalSettings ->
                    settings to generalSettings
                }
                .collect { (settings, generalSettings) ->
                    val cyclicIndexFlow = flow {
                        var cyclicindex = 0
                        while (true) {
                            emit(cyclicindex)
                            if (time(settings[index]).time > 0L)
                            {
                                Timber.d("cyclic CURRENT index is $cyclicindex")
                                cyclicindex = when (cyclicindex) {
                                    0 -> if (secondField(settings[index]).isactive) 1 else if (thirdField(settings[index]).isactive) 2 else 0
                                    1 -> if (thirdField(settings[index]).isactive) 2 else 0
                                    else -> 0
                                }
                                delay(time(settings[index]).time )
                            }
                        }
                    }.distinctUntilChanged()

                    val firstFieldFlow =
                        karooSystem.streamDataFlow(firstField(settings[index]).kaction.action)
                    val secondFieldFlow =
                        karooSystem.streamDataFlow(secondField(settings[index]).kaction.action)
                    val thirdFieldFlow =
                        karooSystem.streamDataFlow(thirdField(settings[index]).kaction.action)

                    combine(
                        firstFieldFlow,
                        secondFieldFlow,
                        thirdFieldFlow,
                        cyclicIndexFlow
                    ) { firstField, secondField, thirdField, index ->
                        Triple(firstField, secondField, thirdField) to index
                    }.collect { (fields, fieldindex) ->

                        val (firstFieldState, secondFieldState, thirdFieldState) = fields

                        val field = when (fieldindex) {
                            0 -> firstField
                            1 -> secondField
                            2 -> thirdField
                            else -> firstField
                        }
                        val valuestream = when (fieldindex) {
                            0 -> firstFieldState
                            1 -> secondFieldState
                            2 -> thirdFieldState
                            else -> firstFieldState
                        }
                        val value = convertValue(
                            valuestream,
                            field(settings[index]).kaction.convert,
                            userProfile.preferredUnit.distance,
                            field(settings[index]).kaction.action
                        )

                        val iconcolor = getColorFilter(
                            context,
                            field(settings[index]).kaction,
                            field(settings[index]).iszone
                        )

                        val colorzone = getColorZone(
                            context,
                            field(settings[index]).kaction.zone,
                            value,
                            userProfile,
                            generalSettings.ispalettezwift
                        ).takeIf {
                            (field(settings[index]).kaction.zone == "heartRateZones" || field(
                                settings[index]
                            ).kaction.zone == "powerZones" || field(settings[index]).kaction.zone == "slopeZones") && field(
                                settings[index]
                            ).iszone
                        } ?: ColorProvider(Color.White, Color.Black)

                        val size = getFieldSize(config.gridSize.second)

                        Timber.d("kaction is "+  field(settings[index]).kaction.label)
                        Timber.d("iconcolor is "+ iconcolor)

                        val result = glance.compose(context, DpSize.Unspecified) {
                            RollingFieldScreen(value,field(settings[index]).kaction.icon,iconcolor,colorzone,size, karooSystem.hardwareType == HardwareType.KAROO,
                                field(settings[index]).kaction.label,generalSettings.iscenterrolling)

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