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
import kotlinx.coroutines.flow.Flow

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    companion object {
        private const val PREVIEW_DELAY = 3000L
        private const val RETRY_DELAY_SHORT = 2000L
        private const val RETRY_DELAY_LONG = 10000L
        private const val EXTRA_ROLLINNG = 350L
    }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time + EXTRA_ROLLINNG else RefreshTime.HALF.time + EXTRA_ROLLINNG


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
            delay(PREVIEW_DELAY)
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
            val settings = context.streamOneFieldSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), listOf(OneFieldSettings()))
            val generalSettings = context.streamGeneralSettings()
                .stateIn(scope, SharingStarted.WhileSubscribed(), GeneralSettings())

            val  cyclicIndexFlow = settings.flatMapLatest { settings ->

                    if (settings.isNotEmpty() && globalIndex in settings.indices && rollingtime(settings[globalIndex]).time > 0L) {
                        flow {
                            var cyclicindex = 0
                            while (true) {
                                val currentSetting = settings[globalIndex]
                                emit(cyclicindex)
                                cyclicindex = when (cyclicindex) {
                                    0 -> if (secondField(currentSetting).isactive) 1 else if (thirdField(
                                            currentSetting
                                        ).isactive
                                    ) 2 else 0

                                    1 -> if (thirdField(currentSetting).isactive) 2 else 0
                                    else -> 0
                                }
                                //Timber.d("cyclicindex: $cyclicindex  RollingTime${rollingtime(currentSetting).time}")
                                delay(rollingtime(currentSetting).time)
                            }
                        }.flowOn(Dispatchers.IO).distinctUntilChanged().catch { e ->
                            Timber.e(e, "Error in cyclicIndexFlow")
                            emit(0)
                        }
                    } else {
                        flowOf(0)
                    }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), 0)

            val combinedFlow = if (config.preview) {
                combine(flowOf(previewOneFieldSettings), flowOf(GeneralSettings()), flowOf(0)) { settings, generalSettings, cyclicIndex ->
                    Triple(settings, generalSettings, cyclicIndex)
                }
            } else {
                combine(settings, generalSettings, cyclicIndexFlow) { settings, generalSettings, cyclicIndex ->
                    Triple(settings, generalSettings, cyclicIndex)
                }
            }


           combinedFlow
               .flatMapLatest { (settings, generalSetting, cyclicIndex) ->


                //Timber.d("IN lastflowmap")
                val currentSetting = settings[globalIndex]
                val primaryField = firstField(currentSetting)
                val secondaryField = secondField(currentSetting)
                val thirdField = thirdField(currentSetting)

                val headwindFlow =
                    if (listOf(primaryField, secondaryField,thirdField).any { it.kaction.name == "HEADWIND" } && generalSetting.isheadwindenabled && !config.preview)
                        createHeadwindFlow(karooSystem,refreshTime) else null

                val firstFieldFlow = if (!config.preview) getFieldFlow(karooSystem,primaryField, headwindFlow, generalSetting,refreshTime) else previewFlow()
                val secondFieldFlow= if(!config.preview) getFieldFlow(karooSystem,secondaryField, headwindFlow, generalSetting,refreshTime) else previewFlow()
                val thirdFieldFlow= if(!config.preview) getFieldFlow(karooSystem,thirdField, headwindFlow, generalSetting,refreshTime) else previewFlow()


                combine(firstFieldFlow, secondFieldFlow, thirdFieldFlow) { firstField, secondField, thirdField ->
                    Triple(firstField, secondField, thirdField)
                }.map { (firstFieldState, secondFieldState, thirdFieldState) ->
                    Triple(firstFieldState, secondFieldState, thirdFieldState) to Triple(settings, generalSetting, cyclicIndex)
                }.catch { e ->
                    Timber.e(e, "Error in combined flow")
                    //emit(Triple<StreamState, StreamState, StreamState>(firstFieldState, secondFieldState, thirdFieldStat) to Triple(settings, generalSetting, cyclicIndex))
                }.buffer()
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

                  // delay(if (karooSystem.hardwareType == HardwareType.K2) RefreshTime.MID.time else RefreshTime.HALF.time)
             // Timber.d("Selector = $selector Field " + field(settings[globalIndex]).kaction + " Size = $size, Value = $value, IconColor = $iconcolor, ColorZone = $colorzone, WindText = $windtext, WindDiff = $winddiff, BaseBitmap = $baseBitmap, Config = $config")
                glance.compose(context, DpSize.Unspecified) {
                    RollingFieldScreen(value, !(field(settings[globalIndex]).kaction.convert == "speed" || field(settings[globalIndex]).kaction.zone == "slopeZones" || field(settings[globalIndex]).kaction.label == "IF"),field(settings[globalIndex]).kaction, iconcolor, colorzone, size, karooSystem.hardwareType == HardwareType.KAROO,
                        generalSetting.iscenteralign,windtext, winddiff.roundToInt(), baseBitmap,selector,config.textSize,iszone,config.preview)
                }.remoteViews


            }.retryWhen { cause, attempt ->
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