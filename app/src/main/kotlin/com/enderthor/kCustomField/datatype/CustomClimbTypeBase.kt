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
import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.Job
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter

import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.streamClimbFieldSettings

import com.enderthor.kCustomField.extensions.streamUserProfile
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig

import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel

import kotlinx.coroutines.flow.catch

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

import timber.log.Timber

import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomClimbTypeBase(
    private val karooSystem: KarooSystemService,
    datatype: String,
    private val globalIndex: Int
) : DataTypeImpl("kcustomfield", datatype) {


    private val glance = GlanceRemoteViews()
    private val firstField = { settings: ClimbFieldSettings -> settings.onefield }
    private val secondField = { settings: ClimbFieldSettings -> settings.secondfield }
    private val thirdField = { settings: ClimbFieldSettings -> settings.thirdfield }
    private val fourthField = { settings: ClimbFieldSettings -> settings.fourthfield }
    private val climbField = { settings: ClimbFieldSettings -> settings.climbfield }
    private val climbOnField = { settings: ClimbFieldSettings -> settings.climbOnfield }
    private val isAlwaysClimbPos = { settings: ClimbFieldSettings -> settings.isAlwaysClimbPos }
    private val isfirsthorizontal = { settings: ClimbFieldSettings -> settings.isfirsthorizontal }
    private val issecondhorizontal = { settings: ClimbFieldSettings -> settings.issecondhorizontal }

    private val refreshTime: Long
        get() = when (karooSystem.hardwareType) {
            HardwareType.K2 -> RefreshTime.MID.time
            else -> RefreshTime.HALF.time
        }.coerceAtLeast(100L)

    

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
    }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("CLIMB StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")

        val scopeJob = Job()
        val scope = CoroutineScope(Dispatchers.IO + scopeJob)

        var isAlwaysclimbOnEnabled = true
        var isfirsthorizontal = true
        var issecondhorizontal = true
        var isOnClimb = false

        ViewState.setCancelled(false)

        val dataflow = context.streamClimbFieldSettings()
            .onStart {
                Timber.d("Iniciando streamClimbFieldSettings")
                emit(previewClimbFieldSettings as MutableList<ClimbFieldSettings>)
            }
            .combine(
                context.streamGeneralSettings()
                    .onStart {
                        Timber.d("Iniciando streamGeneralSettings")
                        emit(GeneralSettings())
                    }
            ) { settings, generalSettings ->
                settings to generalSettings
            }.combine(
                karooSystem.streamUserProfile()

            ) { (settings, generalSettings), userProfile ->
                ClimbGlobalConfigState(settings, generalSettings, userProfile)
            }



        val configjob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)
        val viewjob = scope.launch {
            try {
                Timber.d("CLIMB Starting view: $extension $globalIndex ")

                try {

                    if (!config.preview) {
                            try {
                                val initialRemoteViews = withContext(Dispatchers.Main) {
                                    glance.compose(context, DpSize.Unspecified) {
                                        NotSupported("Searching ...",21)
                                    }.remoteViews
                                }
                                withContext(Dispatchers.Main) {
                                    emitter.updateView(initialRemoteViews)
                                }


                            } catch (e: Exception) {
                                Timber.e(e, "CLIMB Error en vista inicial: $extension $globalIndex ")
                            }
                        delay(400L + (Random.nextInt(10) * 150L))

                    }

                    Timber.d("CLIMBStarting view flow: $extension $globalIndex  karooSystem@$karooSystem ")


                    dataflow.flatMapLatest { state ->
                        val (settings, generalSettings, userProfile) = state

                        if (userProfile == null) {
                            Timber.d("CLIMB UserProfile no disponible")
                            return@flatMapLatest flowOf(
                                Triple(
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    state
                                )
                            )
                        }

                        val currentSettings = settings.getOrNull(globalIndex)
                            ?: throw IndexOutOfBoundsException("Invalid index $globalIndex")

                        val primaryField = firstField(currentSettings)
                        val secondaryField = secondField(currentSettings)
                        val thirdField = thirdField(currentSettings)
                        val fourthField = fourthField(currentSettings)
                        val climbField = climbField(currentSettings)
                        val climbOnField = climbOnField(currentSettings)

                        isAlwaysclimbOnEnabled = isAlwaysClimbPos(currentSettings)
                        isfirsthorizontal = isfirsthorizontal(currentSettings)
                        issecondhorizontal = issecondhorizontal(currentSettings)


                        val headwindFlow =
                            if (listOf(
                                    primaryField,
                                    secondaryField
                                ).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled
                            )
                                createHeadwindFlow(karooSystem, refreshTime) else flowOf(
                                StreamHeadWindData(0.0, 0.0)
                            )

                        val firstFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            primaryField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()
                        val secondFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            secondaryField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()
                        val thirdFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            thirdField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()
                        val fourthFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            fourthField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()

                        val climbStartFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            climbField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()
                        val climbOnFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            climbOnField,
                            headwindFlow,
                            generalSettings,
                            context
                        ) else previewFlow()


                        val climbFieldFlow =
                            (if (!config.preview)
                                karooSystem.getFieldFlow(
                                    DataType.Type.ELEVATION_TO_TOP,
                                    headwindFlow,
                                    generalSettings,
                                    context
                                )
                            else previewFlow()
                                    ).flatMapLatest { state ->
                                    val elevationToTop = when (state) {
                                        is StreamState.Streaming -> state.dataPoint.values[DataType.Field.SINGLE]
                                            ?: 0.0

                                        else -> 0.0
                                    }

                                    isOnClimb = elevationToTop > 0.0

                                    if (elevationToTop > 0.0) climbOnFieldFlow else climbStartFieldFlow
                                }


                        val combinedFlow1 = combine(
                            firstFieldFlow,
                            secondFieldFlow,
                            thirdFieldFlow
                        ) { first: Any, second: Any, third: Any ->
                            Triple(first, second, third)
                        }
                        val combinedFlow2 = combine(
                            fourthFieldFlow,
                            climbFieldFlow,
                        ) { fourth: Any, climb: Any -> fourth to climb }

                        combine(
                            combinedFlow1,
                            combinedFlow2
                        ) { triple1, duple ->
                            val (firstState, secondState, thirdState) = triple1
                            val (fourthState, climbState) = duple

                            ClimbResultData(
                                firstState,
                                secondState,
                                thirdState,
                                fourthState,
                                climbState,
                                state
                            )
                        }

                    }.onEach { result ->


                        val (firstFieldState, secondFieldState, thirdFieldState,
                            fourthFieldState, climbFieldState, globalConfig) = result as ClimbResultData

                        val setting = globalConfig.settings
                        val generalSettings = globalConfig.generalSettings
                        val userProfile = globalConfig.userProfile

                        if (userProfile == null) {
                            Timber.d("UserProfile no disponible")
                            return@onEach
                        }
                        val settings = setting[globalIndex]

                        val (firstvalue, firstIconcolor, firstColorzone, isleftzone, firstvalueRight) = getFieldState(
                            firstFieldState,
                            firstField(settings),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )

                        val (secondvalue, secondIconcolor, secondColorzone, isrightzone, secondvalueRight) = getFieldState(
                            secondFieldState,
                            secondField((settings)),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )
                        val (thirdvalue, thirdIconcolor, thirdColorzone, isleftzone2, thirdvalueRight) = getFieldState(
                            thirdFieldState,
                            thirdField(settings),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )
                        val (fourthvalue, fourthIconcolor, fourthColorzone, isrightzone2, fourthvalueRight) = getFieldState(
                            fourthFieldState,
                            fourthField(settings),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )
                        val (climbvalue, climbIconcolor, climbColorzone, isleftzone3, climbvalueRight) = getFieldState(
                            climbFieldState,
                            if(isOnClimb) climbOnField(settings) else climbField(settings),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )



                        val (winddiff, windtext) = if (firstFieldState !is StreamState || secondFieldState !is StreamState) {
                            val windData = (firstFieldState as? StreamHeadWindData)
                                ?: (secondFieldState as StreamHeadWindData)
                            windData.diff to windData.windSpeed.roundToInt().toString()
                        } else 0.0 to ""


                        val clayout = when {
                            generalSettings.iscenterkaroo -> when (config.alignment) {
                                ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                            }

                            else -> generalSettings.iscenteralign
                        }

                        if (isAlwaysclimbOnEnabled) {
                            isfirsthorizontal = false
                            issecondhorizontal = false
                        }

                        Timber.d("size field config $config")
                        try {
                            val newView = withContext(Dispatchers.Main) {
                                glance.compose(context, DpSize.Unspecified) {
                                    ClimbScreenSelector(
                                        firstvalue,
                                        secondvalue,
                                        thirdvalue,
                                        fourthvalue,
                                        climbvalue,
                                        firstField(settings),
                                        secondField(settings),
                                        thirdField(settings),
                                        fourthField(settings),
                                        climbField(settings),
                                        firstIconcolor,
                                        secondIconcolor,
                                        thirdIconcolor,
                                        fourthIconcolor,
                                        climbIconcolor,
                                        firstColorzone,
                                        secondColorzone,
                                        thirdColorzone,
                                        fourthColorzone,
                                        climbColorzone,
                                        config.viewSize.first,
                                        karooSystem.hardwareType == HardwareType.KAROO,
                                        clayout,
                                        windtext,
                                        winddiff.roundToInt(),
                                        baseBitmap,
                                        generalSettings.isdivider,
                                        firstvalueRight,
                                        secondvalueRight,
                                        thirdvalueRight,
                                        fourthvalueRight,
                                        climbvalueRight,
                                        isfirsthorizontal,
                                        issecondhorizontal,
                                        isAlwaysclimbOnEnabled
                                    )
                                }.remoteViews
                            }

                            // Timber.d("CLIMB Updating view: $extension $globalIndex values: $firstvalue, $secondvalue layout: $clayout")
                            withContext(Dispatchers.Main) {
                                emitter.updateView(newView)
                            }
                            delay(refreshTime)
                        } catch (e: Exception) {
                            Timber.e(
                                e,
                                "CLIMB Error composing/updating view: $extension $globalIndex"
                            )
                        }
                    }
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    Timber.d("CLIMB Flow cancelled: $extension $globalIndex")
                                    throw e
                                }

                                else -> {
                                    Timber.e(e, "CLIMB Flow error: $extension $globalIndex")
                                    throw e
                                }
                            }
                        }
                        .retryWhen { cause, attempt ->

                            if (attempt > 4) {
                                Timber.e(
                                    cause,
                                    "CLIMB Max retries reached: $extension $globalIndex (attempt $attempt) "
                                )

                                delay(Delay.RETRY_LONG.time)
                                //startView(context, config, emitter)
                                true
                            } else {
                                Timber.w(
                                    cause,
                                    "CLIMB Retrying flow: $extension $globalIndex (attempt $attempt) "
                                )
                                delay(Delay.RETRY_SHORT.time)
                                true
                            }

                        }
                        .launchIn(scope)

                } catch (e: CancellationException) {
                    Timber.d("CLIMB View operation cancelled: $extension $globalIndex ")
                    throw e
                } catch (e: DeadObjectException) {
                    Timber.e(e, "CLIMB Dead object en vista principal, parando")
                    scope.cancel()
                }

            } catch (e: Exception) {
                Timber.e(e, "CLIMB ViewJob error: $extension $globalIndex ")
                if (!scope.isActive) return@launch
                delay(1000L)

            }
        }

        emitter.setCancellable {
            try {
                Timber.d("Cancelando todos los jobs y flujos de CLIMB")
                ViewState.setCancelled(true)
                viewjob.cancel()
                configjob.cancel()
                scopeJob.cancel()


                scope.launch {
                    delay(100)
                    if (scope.isActive) {
                        Timber.w("Forzando cancelación del scope de CLIMB")
                        scope.cancel()
                    }
                }
            } catch (e: CancellationException) {

            } catch (e: Exception) {
                Timber.e(e, "Error durante la cancelación de CLIMB")
            }

        }
    }
}