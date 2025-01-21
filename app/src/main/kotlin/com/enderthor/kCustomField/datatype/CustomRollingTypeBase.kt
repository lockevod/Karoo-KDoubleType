package com.enderthor.kCustomField.datatype

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig

import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import com.enderthor.kCustomField.R

import timber.log.Timber


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomRollingTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    protected val globalIndex: Int
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()

    protected val firstField= { settings: OneFieldSettings -> settings.onefield }
    protected val secondField= { settings: OneFieldSettings -> settings.secondfield }
    protected val thirdField= { settings: OneFieldSettings -> settings.thirdfield }
    protected val rollingtime= { settings: OneFieldSettings -> settings.rollingtime }


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("Start Rolling type stream")

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
            delay(if (karooSystem.hardwareType == HardwareType.K2) RefreshTime.MID.time else RefreshTime.HALF.time)
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

                if (settings.isNotEmpty() && globalIndex in settings.indices && rollingtime(settings[globalIndex]).time > 0L) {
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
                            //Timber.d("cyclicindex: $cyclicindex  RollingTime${rollingtime(currentSetting).time}")
                            delay(rollingtime(currentSetting).time)
                        }
                    }.distinctUntilChanged().flowOn(Dispatchers.IO).catch { e ->
                        Timber.e(e, "Error in cyclicIndexFlow")
                        emit(0)
                    }
                } else {
                    flowOf(0)
                }.stateIn(scope, SharingStarted.Lazily, 0)
            }

            combine(settings, generalSettings, cyclicIndexFlow) { settings, generalSettings, cyclicIndex ->
                Triple(settings, generalSettings, cyclicIndex)
            }.flatMapLatest { (settings: List<OneFieldSettings>, generalSetting: GeneralSettings, cyclicIndex) ->
                //Timber.d("IN lastflowmap")
                val currentSetting = settings[globalIndex]
                val primaryField = firstField(currentSetting)
                val secondaryField = secondField(currentSetting)
                val thirdField = thirdField(currentSetting)

                val headwindFlow =
                    if (listOf(primaryField, secondaryField,thirdField).any { it.kaction.name == "HEADWIND" } && generalSetting.isheadwindenabled)
                        createHeadwindFlow(karooSystem) else null

                val firstFieldFlow = getFieldFlow(karooSystem,primaryField, headwindFlow, generalSetting)
                val secondFieldFlow= getFieldFlow(karooSystem,secondaryField, headwindFlow, generalSetting)
                val thirdFieldFlow= getFieldFlow(karooSystem,thirdField, headwindFlow, generalSetting)


                combine(firstFieldFlow, secondFieldFlow, thirdFieldFlow) { firstField, secondField, thirdField ->
                    Triple(firstField, secondField, thirdField)
                }.map { (firstFieldState, secondFieldState, thirdFieldState) ->
                    Triple(firstFieldState, secondFieldState, thirdFieldState) to Triple(settings, generalSetting, cyclicIndex)
                }.catch { e ->
                    Timber.e(e, "Error in combined flow")
                    //emit(Triple<StreamState, StreamState, StreamState>(firstFieldState, secondFieldState, thirdFieldStat) to Triple(settings, generalSetting, cyclicIndex))
                }
            }.map { (fieldStates, settingsData) ->

                val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)
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

                val (value, iconcolor, colorzone,iszone ) = getFieldState(valuestream, field(settings[globalIndex]), context, userProfile, generalSetting.ispalettezwift)

                val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState || thirdFieldState !is StreamState) {
                    val windData = (firstFieldState as? StreamHeadWindData) ?: (secondFieldState as? StreamHeadWindData) ?: (thirdFieldState as StreamHeadWindData)
                    windData.diff to windData.windSpeed.roundToInt().toString()
                } else 0.0 to ""

                val selector: Boolean =  valuestream is StreamState

                val size = getFieldSize(config.gridSize.second)

                /*val widthField = if (karooSystem.hardwareType == HardwareType.KAROO)
                    1.875 * config.viewSize.first
                else
                    config.viewSize.first * context.resources.displayMetrics.densityDpi / 160.0
*/


             // Timber.d("Selector = $selector Field " + field(settings[globalIndex]).kaction + " Size = $size, Value = $value, IconColor = $iconcolor, ColorZone = $colorzone, WindText = $windtext, WindDiff = $winddiff, BaseBitmap = $baseBitmap, Config = $config")
                glance.compose(context, DpSize.Unspecified) {
                    RollingFieldScreen(value, !(field(settings[globalIndex]).kaction.convert == "speed" || field(settings[globalIndex]).kaction.zone == "slopeZones" || field(settings[globalIndex]).kaction.label == "IF"),field(settings[globalIndex]).kaction, iconcolor, colorzone, size, karooSystem.hardwareType == HardwareType.KAROO,
                        generalSetting.iscenteralign,windtext, winddiff.roundToInt(), baseBitmap,selector,config.textSize,iszone)
                }.remoteViews


            }.retryWhen { cause, attempt ->
                Timber.e(cause, "Error collecting flow, retrying... (attempt $attempt)")
                delay(1000)
                true
            }.collect {
                result ->
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