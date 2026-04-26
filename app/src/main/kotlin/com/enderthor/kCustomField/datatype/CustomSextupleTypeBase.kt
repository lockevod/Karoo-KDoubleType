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

import com.enderthor.kCustomField.extensions.streamSextupleFieldSettings
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import com.enderthor.kCustomField.R

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

import kotlinx.coroutines.flow.conflate
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
abstract class CustomSextupleTypeBase(
    private val karooSystem: KarooSystemService,
    datatype: String,
    private val globalIndex: Int
) : DataTypeImpl("kcustomfield", datatype) {


    private val glance = GlanceRemoteViews()
    private val firstField = { settings: SextupleFieldSettings -> settings.onefield }
    private val secondField = { settings: SextupleFieldSettings -> settings.secondfield }
    private val thirdField = { settings: SextupleFieldSettings -> settings.thirdfield }
    private val fourthField = { settings: SextupleFieldSettings -> settings.fourthfield }
    private val fifthField = { settings: SextupleFieldSettings -> settings.fifthfield }
    private val sixthField = { settings: SextupleFieldSettings -> settings.sixthfield }

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
        Timber.d("SEXTUPLE StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")

        val scopeJob = Job()
        val scope = CoroutineScope(Dispatchers.IO + scopeJob)
        ViewState.setCancelled(false)

        val dataflow = context.streamSextupleFieldSettings()
            .onStart {
                Timber.d("Iniciando streamSextupleFieldSettings")
                emit(previewSextupleFieldSettings as MutableList<SextupleFieldSettings>)
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
                SextupleGlobalConfigState(settings, generalSettings, userProfile)
            }



        val configjob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.circle)
        val viewjob = scope.launch {
            try {
                Timber.d("SEXTUPLE Starting view: $extension $globalIndex ")

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
                            Timber.e(e, "SEXTUPLE Error en vista inicial: $extension $globalIndex ")
                        }
                        delay(400L + (Random.nextInt(10) * 150L))

                    }

                    Timber.d("SEXTUPLE Starting view flow: $extension $globalIndex  karooSystem@$karooSystem ")

                    dataflow
                        .flatMapLatest { state ->
                            val (settings, generalSettings, userProfile) = state

                            if (userProfile == null) {
                                Timber.d("SEXTUPLE UserProfile no disponible")
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
                            val fifthField = fifthField(currentSettings)
                            val sixthField = sixthField(currentSettings)

                            val headwindFlow =
                                if (listOf(
                                        primaryField, secondaryField,
                                        thirdField, fourthField,
                                        fifthField, sixthField
                                    ).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled
                                )
                                    createHeadwindFlow(karooSystem, refreshTime) else flowOf(
                                    StreamHeadWindData(0.0, 0.0)
                                )

                            val firstFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                primaryField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val secondFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                secondaryField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val thirdFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                thirdField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val fourthFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                fourthField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val fifthFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                fifthField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val sixthFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                                sixthField,
                                headwindFlow,
                                generalSettings
                            ) else previewFlow()
                            val combinedFlow1 = combine(
                                firstFieldFlow,
                                secondFieldFlow,
                                thirdFieldFlow
                            ) { first: Any, second: Any, third: Any ->
                                Triple(first, second, third)
                            }
                            val combinedFlow2 = combine(
                                fourthFieldFlow,
                                fifthFieldFlow,
                                sixthFieldFlow
                            ) { fourth: Any, fifth: Any, sixth: Any ->
                                Triple(
                                    fourth,
                                    fifth,
                                    sixth
                                )
                            }

                            combine(
                                combinedFlow1,
                                combinedFlow2
                            ) { triple1, triple2 ->
                                val (firstState, secondState, thirdState) = triple1
                                val (fourthState, fifthState, sixthState) = triple2

                                SextupleResultData(
                                    firstState,
                                    secondState,
                                    thirdState,
                                    fourthState,
                                    fifthState,
                                    sixthState,
                                    state
                                )
                            }
                        }.conflate()
                        .onEach { result ->

                        if ( ViewState.isCancelled()) {
                            Timber.d("SEXTUPLE Skipping update, job cancelled: $extension $globalIndex")
                            return@onEach
                        }
                        val (firstFieldState, secondFieldState, thirdFieldState,
                        fourthFieldState, fifthFieldState, sixthFieldState, globalConfig) = result as SextupleResultData

                        val setting = globalConfig.settings
                        val generalSettings = globalConfig.generalSettings
                        val userProfile = globalConfig.userProfile

                        if (userProfile == null) {
                                Timber.d("UserProfile no disponible")
                                return@onEach
                            }
                            val settings = setting[globalIndex]

                            val (firstvalue, firstIconcolor, firstColorzone, _, firstvalueRight) = getFieldState(
                                firstFieldState,
                                firstField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (secondvalue, secondIconcolor, secondColorzone, _, secondvalueRight) = getFieldState(
                                secondFieldState,
                                secondField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (thirdvalue, thirdIconcolor, thirdColorzone, _, thirdvalueRight) = getFieldState(
                                thirdFieldState,
                                thirdField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (fourthvalue, fourthIconcolor, fourthColorzone, _, fourthvalueRight) = getFieldState(
                                fourthFieldState,
                                fourthField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (fifthvalue, fifthIconcolor, fifthColorzone, _, fifthvalueRight) = getFieldState(
                                fifthFieldState,
                                fifthField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )

                            val (sixthvalue, sixthIconcolor, sixthColorzone, _, sixthvalueRight) = getFieldState(
                                sixthFieldState,
                                sixthField(settings),
                                context,
                                userProfile,
                                generalSettings.ispalettezwift
                            )
                            val (winddiff, windtext) = listOf(
                                firstFieldState, secondFieldState,
                                thirdFieldState, fourthFieldState,
                                fifthFieldState, sixthFieldState
                            ).firstOrNull { it is StreamHeadWindData }
                                ?.let { it as StreamHeadWindData }
                                ?.let { it.diff to it.windSpeed.roundToInt().toString() }
                                ?: (0.0 to "")

                            val fieldNumber = 2

                            val clayout = when {
                                generalSettings.iscenterkaroo -> when (config.alignment) {
                                    ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                    ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                    ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                                }
                                else -> generalSettings.iscentervertical
                            }

                            try {
                                if ( ViewState.isCancelled()) {
                                    Timber.d("SEXTUPLE Skipping composition, job cancelled: $extension $globalIndex")
                                    return@onEach
                                }
                                val newView = withContext(Dispatchers.Main) {
                                    if ( ViewState.isCancelled()) {
                                        return@withContext null
                                    }
                                    glance.compose(context, DpSize.Unspecified) {
                                        SextupleScreenSelector(
                                            fieldNumber,
                                            false,
                                            firstvalue,
                                            secondvalue,
                                            thirdvalue,
                                            fourthvalue,
                                            fifthvalue,
                                            sixthvalue,
                                            firstField(settings),
                                            secondField(settings),
                                            thirdField(settings),
                                            fourthField(settings),
                                            fifthField(settings),
                                            sixthField(settings),
                                            firstIconcolor,
                                            secondIconcolor,
                                            thirdIconcolor,
                                            fourthIconcolor,
                                            fifthIconcolor,
                                            sixthIconcolor,
                                            firstColorzone,
                                            secondColorzone,
                                            thirdColorzone,
                                            fourthColorzone,
                                            fifthColorzone,
                                            sixthColorzone,
                                            getFieldSize(config.gridSize.second),
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
                                            fifthvalueRight,
                                            sixthvalueRight
                                        )
                                    }.remoteViews
                                }
                                if (newView == null) return@onEach

                                withContext(Dispatchers.Main) {
                                    if ( ViewState.isCancelled()) return@withContext
                                    emitter.updateView(newView)
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) {
                                    Timber.d("SEXTUPLE View update cancelled normally: $extension $globalIndex")
                                } else {
                                    Timber.e(e, "SEXTUPLE Error composing/updating view: $extension $globalIndex")
                                    if (coroutineContext.isActive && !ViewState.isCancelled()) {
                                        throw e
                                    }
                                }
                            }
                        }
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    Timber.d("SEXTUPLE Flow cancelled: $extension $globalIndex")
                                    throw e
                                }
                                else -> {
                                    Timber.e(e, "SEXTUPLE Flow error: $extension $globalIndex")
                                    throw e
                                }
                            }
                        }
                        .retryWhen { cause, attempt ->

                            when {

                                cause is CancellationException && ViewState.isCancelled() -> {
                                    Timber.d("SEXTUPLE No se reintenta el flujo cancelado por el emitter: $extension $globalIndex")
                                    false
                                }
                                attempt > 4 -> {
                                    Timber.e(cause, "SEXTUPLE Max retries reached: $extension $globalIndex (attempt $attempt)")
                                    delay(Delay.RETRY_LONG.time)
                                    true
                                }
                                else -> {
                                    Timber.w(cause, "SEXTUPLE Retrying flow: $extension $globalIndex (attempt $attempt)")
                                    delay(Delay.RETRY_SHORT.time)
                                    true
                                }
                            }

                        }
                        .launchIn(scope)

                } catch (e: CancellationException) {
                    Timber.d("SEXTUPLE View operation cancelled: $extension $globalIndex ")
                    throw e
                }
                catch (e: DeadObjectException) {
                    Timber.e(e, "SEXTUPLE Dead object en vista principal, parando")
                    scope.cancel()
                }

          } catch (e: Exception) {
                Timber.e(e, "SEXTUPLE ViewJob error: $extension $globalIndex ")
                if (!scope.isActive) return@launch
                delay(1000L)

            }
        }

        emitter.setCancellable {
            try {
                if (config.preview) return@setCancellable

                Timber.d("SEXTUPLE Emitter.setCancellable: extension=$extension index=$globalIndex")

                ViewState.setCancelled(true)
                configjob.cancel()
                viewjob.cancel()
                scope.cancel()
                scopeJob.cancel()

            } catch (_: CancellationException) {
                // normal
            } catch (e: Exception) {
                Timber.e(e, "SEXTUPLE Error durante la cancelación: $extension $globalIndex")
            }

        }
    }
}