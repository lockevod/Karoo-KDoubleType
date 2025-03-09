package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.BitmapFactory
import android.os.DeadObjectException
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
import kotlinx.coroutines.Job
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.streamOneFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomRollingTypeBase(
    private val karooSystem: KarooSystemService,
    datatype: String,
    private val index: Int
) : DataTypeImpl("kcustomfield", datatype) {


    private val glance = GlanceRemoteViews()
    private val firstField = { settings: OneFieldSettings -> settings.onefield }
    private val secondField = { settings: OneFieldSettings -> settings.secondfield }
    private val thirdField = { settings: OneFieldSettings -> settings.thirdfield }
    private val rollingtime = { settings: OneFieldSettings -> settings.rollingtime }
    private val isextratime = { settings: OneFieldSettings -> settings.isextratime }

    private val refreshTime: Long
        get() = if (karooSystem.hardwareType == HardwareType.K2)
            RefreshTime.MID.time + RefreshTime.EXTRA_ROLLING.time else RefreshTime.HALF.time + RefreshTime.EXTRA_ROLLING.time



   /* override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("Starting Rolling type stream")
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
                Timber.e(e, "Stream Rolling error occurred")
                emitter.onError(e)
            }
        }.also { job ->
            emitter.setCancellable {
                Timber.d("Stopping Rolling stream")
                job.cancel()
            }
        }
    }
*/
    private fun previewFlow(): Flow<StreamState> = flow {
        while (true) {
            emit(StreamState.Streaming(
                DataPoint(
                    dataTypeId,
                    mapOf(DataType.Field.SINGLE to (0..100).random().toDouble()),
                    extension
                )
            ))
            delay(Delay.PREVIEW.time)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("ROLLING StartView: field $extension index $index field $dataTypeId config: $config")

       // emitterId = emitter.toString().substringAfter("@")
        val scopeJob = Job()
        val scope = CoroutineScope(Dispatchers.IO + scopeJob)
        ViewState.setCancelled(false)


        //cleanupJobs()

        val globalIndex = index

        val configjob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)

        val viewjob = scope.launch {


            try {
                val userProfile = karooSystem.consumerFlow<UserProfile>().first()
                val settings = context.streamOneFieldSettings()
                    .stateIn(
                        scope,
                        SharingStarted.WhileSubscribed(4000),
                        listOf(OneFieldSettings())
                    )

                val generalSettings = context.streamGeneralSettings()
                    .stateIn(scope, SharingStarted.WhileSubscribed(6000), GeneralSettings())
                delay(50L + (Random.nextInt(4) * 15L))

                try {
                    if (!config.preview) {
                        withContext(Dispatchers.Main) {
                            val startedRemoteViews = glance.compose(context, DpSize.Unspecified) {
                                NotSupported("Searching...", 22)
                            }.remoteViews
                            emitter.updateView(startedRemoteViews)
                        }

                        delay(600L + (Random.nextInt(5) * 100L))
                    }
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "ROLLING Error en vista inicial: $extension $globalIndex "
                    )
                }


                retryFlow(
                    action = {
                        val settingsFlow = combine(
                            settings,
                            generalSettings
                        ) { settings, generalSettings -> settings to generalSettings }
                            .firstOrNull { (settings, _) -> globalIndex in settings.indices }

                        if (settingsFlow != null) {
                            Timber.d("ROLLING INITIAL RETRYFLOW encontrado: $index  campo: $dataTypeId ")
                            val (settings, generalSettings) = settingsFlow
                            emit(settings to generalSettings)
                        } else {
                            Timber.e("ROLLING Index out of bounds: $globalIndex ")
                            throw IndexOutOfBoundsException("GlobalIndex out of bounds")
                        }
                    },
                    onFailure = { attempts, e ->
                        Timber.e("Not valid Rolling index in $attempts attempts. Error: $e ")
                        emit(listOf(OneFieldSettings()) to GeneralSettings())
                    }
                )

                val cyclicIndexFlow = settings.flatMapLatest { settings ->
                    if (settings.isNotEmpty() && globalIndex in settings.indices && rollingtime(
                            settings[globalIndex]
                        ).time > 0L
                    ) {
                        flow {
                            var cyclicindex = 0
                            val currentSetting = settings[globalIndex]
                            while (true) {
                                emit(cyclicindex)
                                val delayFactor =
                                    if (isextratime(currentSetting) && cyclicindex == 0) 3
                                    else 1

                                cyclicindex = when (cyclicindex) {
                                    0 -> if (secondField(currentSetting).isactive) 1 else if (thirdField(
                                            currentSetting
                                        ).isactive
                                    ) 2 else 0

                                    1 -> if (thirdField(currentSetting).isactive) 2 else 0
                                    else -> 0
                                }

                                delay(delayFactor* rollingtime(currentSetting).time)
                            }
                        }.flowOn(Dispatchers.IO).distinctUntilChanged().catch { e ->
                            Timber.e(e, "Error in cyclicIndexFlow")
                            emit(0)
                        }
                    } else {
                        flowOf(0)
                    }
                }.stateIn(scope, SharingStarted.WhileSubscribed(), 0)

                val combinedFlow = combine(
                    settings,
                    generalSettings,
                    cyclicIndexFlow
                ) { settings, generalSettings, cyclicIndex ->
                    Triple(settings, generalSettings, cyclicIndex)
                }.distinctUntilChanged()

                combinedFlow
                    .flatMapLatest { (settings, generalSetting, cyclicIndex) ->
                        val currentSetting = settings.getOrNull(globalIndex)
                            ?: return@flatMapLatest flowOf(
                                Triple(
                                    previewOneFieldSettings,
                                    GeneralSettings(),
                                    0
                                ) to Triple(settings, generalSetting, cyclicIndex)
                            )

                        val primaryField = firstField(currentSetting)
                        val secondaryField = secondField(currentSetting)
                        val thirdField = thirdField(currentSetting)

                        val headwindFlow =
                            if (listOf(
                                    primaryField,
                                    secondaryField,
                                    thirdField
                                ).any { it.kaction.name == "HEADWIND" } && generalSetting.isheadwindenabled && !config.preview
                            )
                                createHeadwindFlow(karooSystem, refreshTime) else flowOf(
                                StreamHeadWindData(0.0, 0.0)
                            )

                        val firstFieldFlow = if (!config.preview)  karooSystem.getFieldFlow(
                            primaryField,
                            headwindFlow,
                            generalSetting,
                            context
                        ) else previewFlow()
                        val secondFieldFlow = if (!config.preview)  karooSystem.getFieldFlow(
                            secondaryField,
                            headwindFlow,
                            generalSetting,
                            context
                        ) else previewFlow()
                        val thirdFieldFlow = if (!config.preview)  karooSystem.getFieldFlow(
                            thirdField,
                            headwindFlow,
                            generalSetting,
                            context
                        ) else previewFlow()

                        combine(
                            firstFieldFlow,
                            secondFieldFlow,
                            thirdFieldFlow
                        ) { firstField, secondField, thirdField ->
                            Triple(firstField, secondField, thirdField)
                        }.map { (firstFieldState, secondFieldState, thirdFieldState) ->
                            Triple(firstFieldState, secondFieldState, thirdFieldState) to Triple(
                                settings,
                                generalSetting,
                                cyclicIndex
                            )
                        }
                    }
                    .onEach { (fieldStates, settingsData) ->


                        if (globalIndex !in settingsData.first.indices) {
                            Timber.e("ROLLING Index out of bounds: $globalIndex, Size: ${settingsData.first.size}")
                            return@onEach
                        }

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

                        val (value, iconcolor, colorzone, iszone, valueSecond) = getFieldState(
                            valuestream,
                            field(settings[globalIndex]),
                            context,
                            userProfile,
                            generalSetting.ispalettezwift
                        )

                        val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState || thirdFieldState !is StreamState) {
                            val windData = (firstFieldState as? StreamHeadWindData)
                                ?: (secondFieldState as? StreamHeadWindData)
                                ?: (thirdFieldState as StreamHeadWindData)
                            windData.diff to windData.windSpeed.roundToInt().toString()
                        } else 0.0 to ""

                        val selector: Boolean = valuestream is StreamState

                        try {
                            val newView = withContext(Dispatchers.Main) {
                                glance.compose(context, DpSize.Unspecified) {
                                    RollingFieldScreen(
                                        value,
                                        !(field(settings[globalIndex]).kaction.convert == "speed" ||
                                                field(settings[globalIndex]).kaction.zone == "slopeZones" ||
                                                field(settings[globalIndex]).kaction.label == "IF"),
                                        field(settings[globalIndex]).kaction,
                                        iconcolor,
                                        colorzone,
                                        getFieldSize(config.gridSize.second),
                                        karooSystem.hardwareType == HardwareType.KAROO,
                                        generalSetting.iscenteralign,
                                        windtext,
                                        winddiff.roundToInt(),
                                        baseBitmap,
                                        selector,
                                        config.textSize,
                                        iszone,
                                        config.preview,
                                        valueSecond
                                    )
                                }.remoteViews
                            }
                            Timber.d("ROLLING Updating view: $extension $index cyclic: $cyclicIndex value: $value ")
                            withContext(Dispatchers.Main) {
                                emitter.updateView(newView)
                            }
                            delay(refreshTime)

                        } catch (e: Exception) {
                            Timber.e(e, "ROLLING Error composing/updating view: $extension $index")
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "ROLLING Flow error: $extension $index")
                    }
                    .retryWhen { cause, attempt ->
                        if (attempt > 3) {
                            Timber.e(
                                cause,
                                "Error collecting Rolling flow, stopping.. (attempt $attempt) Cause: $cause "
                            )
                            // cleanupJobs()
                            delay(Delay.RETRY_LONG.time)
                            // startView(context, config, emitter)
                            true
                        } else {
                            Timber.e(
                                cause,
                                "Error collecting Rolling flow, retrying... (attempt $attempt) Cause: $cause "
                            )
                            delay(Delay.RETRY_SHORT.time)
                            true
                        }
                    }
                    .launchIn(scope)
            } catch (e: DeadObjectException) {
                    Timber.e(e, "ROLLING Dead object en vista principal, parando")
                    scope.cancel()

            } catch (e: Exception) {
                Timber.e(e, "ROLLING ViewJob error: $extension $index ")

                //delay(1000)
                //startView(context, config, emitter)
            }
        }

        emitter.setCancellable {
            try {
                Timber.d("Cancelando todos los jobs y flujos")
                ViewState.setCancelled(true)
                viewjob.cancel()
                configjob.cancel()
                scopeJob.cancel() // Esto cancelará todos los flujos hijos

                // Esperar brevemente para asegurar la cancelación
                scope.launch {
                    delay(100)
                    if (scope.isActive) {
                        Timber.w("Forzando cancelación del scope")
                        scope.cancel()
                    }
                }
            } catch (e: CancellationException) {
                // Cancelación normal, no necesita logging
            } catch (e: Exception) {
                Timber.e(e, "Error during view cancellation ")
            }
        }
    }
}