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
import kotlinx.coroutines.flow.*
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import timber.log.Timber

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomRollingTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    private val context: Context,
    protected val globalIndex: Int
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()

    protected val firstField= { settings: OneFieldSettings -> settings.onefield }
    protected val secondField= { settings: OneFieldSettings -> settings.secondfield }
    protected val thirdField= { settings: OneFieldSettings -> settings.thirdfield }
    protected val time= { settings: OneFieldSettings -> settings.rollingtime }


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")

        val job = CoroutineScope(Dispatchers.IO).launch {
            //context.streamGeneralSettings().collect {
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId,
                            mapOf(DataType.Field.SINGLE to 1.0),
                            extension
                        )
                    )
                )
            delay(if (karooSystem.hardwareType == HardwareType.KAROO) RefreshTime.MID.time else RefreshTime.HALF.time)
           // }
        }
        emitter.setCancellable {
            Timber.d("stop speed stream")
            job.cancel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(Dispatchers.IO)

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }

        val job = scope.launch {
            val userProfile = karooSystem.consumerFlow<UserProfile>().first()
            val settings = context.streamOneFieldSettings().stateIn(scope, SharingStarted.Lazily, listOf(OneFieldSettings()))
            val generalSettings = context.streamGeneralSettings().stateIn(scope, SharingStarted.Lazily, GeneralSettings())

            val cyclicIndexFlow = settings.flatMapLatest { settings ->
                if (settings.isNotEmpty() && globalIndex in settings.indices && time(settings[globalIndex]).time > 0L) {
                    flow {
                        var cyclicindex = 0
                        while (true) {
                            val currentSetting = settings[globalIndex]
                            emit(cyclicindex)
                            cyclicindex = when (cyclicindex) {
                                0 -> if (secondField(currentSetting).isactive) 1 else if (thirdField(currentSetting).isactive) 2 else 0
                                1 -> if (thirdField(currentSetting).isactive) 2 else 0
                                else -> 0
                            }
                            delay(time(currentSetting).time)
                        }
                    }.distinctUntilChanged().flowOn(Dispatchers.IO)
                } else {
                    flowOf(0)
                }.stateIn(scope, SharingStarted.Lazily, 0)
            }

            combine(settings, generalSettings, cyclicIndexFlow) { settings, generalSettings, cyclicIndex ->
                Triple(settings, generalSettings, cyclicIndex)
            }.flatMapLatest { (settings: List<OneFieldSettings>, generalSetting: GeneralSettings, cyclicIndex) ->
                val currentSetting = settings[globalIndex]

                val firstFieldFlow = karooSystem.streamDataFlow(firstField(currentSetting).kaction.action)
                val secondFieldFlow = karooSystem.streamDataFlow(secondField(currentSetting).kaction.action)
                val thirdFieldFlow = karooSystem.streamDataFlow(thirdField(currentSetting).kaction.action)

                combine(firstFieldFlow, secondFieldFlow, thirdFieldFlow) { firstField, secondField, thirdField ->
                    Triple(firstField, secondField, thirdField)
                }.map { (firstFieldState, secondFieldState, thirdFieldState) ->
                    Triple(firstFieldState, secondFieldState, thirdFieldState) to Triple(settings, generalSetting, cyclicIndex)
                }
            }.map { (fieldStates, settingsData) ->

                val (firstFieldState, secondFieldState, thirdFieldState) = fieldStates
                val (settings, generalSetting, cyclicIndex) = settingsData

                val field = when (cyclicIndex) {
                    0 -> firstField
                    1 -> secondField
                    2 -> thirdField
                    else -> firstField
                }
                val valuestream = when (cyclicIndex) {
                    0 -> firstFieldState
                    1 -> secondFieldState
                    2 -> thirdFieldState
                    else -> firstFieldState
                }
                val value = convertValue(
                    valuestream,
                    field(settings[globalIndex]).kaction.convert,
                    userProfile.preferredUnit.distance,
                    field(settings[globalIndex]).kaction.action
                )

                val iconcolor = getColorFilter(
                    context,
                    field(settings[globalIndex]).kaction,
                    field(settings[globalIndex]).iszone
                )
                //Timber.d("generalSettings: $generalSettings")
                val colorzone = getColorZone(
                    context,
                    field(settings[globalIndex]).kaction.zone,
                    value,
                    userProfile,
                    generalSetting.ispalettezwift
                ).takeIf {
                    (field(settings[globalIndex]).kaction.zone == "heartRateZones" || field(settings[globalIndex]).kaction.zone == "powerZones" || field(settings[globalIndex]).kaction.zone == "slopeZones") && field(settings[globalIndex]).iszone
                } ?: ColorProvider(Color.White, Color.Black)

                val size = getFieldSize(config.gridSize.second)

                Timber.d("Field " + field(settings[globalIndex]).kaction + " $value, iconcolor: $iconcolor, colorzone: $colorzone, size: $size")

              //  Timber.d("geneal Setting " +generalSetting.iscenterrolling)
                val result = glance.compose(context, DpSize.Unspecified) {
                    RollingFieldScreen(value, field(settings[globalIndex]).kaction.icon, iconcolor, colorzone, size, karooSystem.hardwareType == HardwareType.KAROO,
                        field(settings[globalIndex]).kaction.label, generalSetting.iscenterrolling)
                }
                emitter.updateView(result.remoteViews)

            }.retryWhen { cause, attempt ->
                Timber.e(cause, "Error collecting flow, retrying... (attempt $attempt)")
                delay(1000)
                true
            }.launchIn(scope)
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            job.cancel()
        }
    }
}