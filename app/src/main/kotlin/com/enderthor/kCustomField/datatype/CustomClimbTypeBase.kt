package com.enderthor.kCustomField.datatype

import android.content.Context
import android.graphics.Bitmap
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

import kotlinx.coroutines.flow.MutableStateFlow
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
import com.enderthor.kCustomField.extensions.streamDataFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

import timber.log.Timber

import java.util.concurrent.atomic.AtomicBoolean
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
    // StateFlow, no un @Volatile suelto: el render depende de este valor, así que tiene que
    // ENTRAR en el flujo combinado. Como campo volátil, un cambio de isOnClimb no emitía nada
    // por sí solo y la transición a subida se quedaba esperando a que emitiera otro stream.
    private val isOnClimbFlow = MutableStateFlow(false)
    // Decodificado una vez por instancia: startView() se re-entra muy rápido en cambios
    // de página/perfil y re-decodificar el recurso en cada entrada es trabajo inútil.
    @Volatile private var cachedBaseBitmap: Bitmap? = null
    // Scope del último preview servido por esta instancia, para poder cancelarlo cuando llega
    // el siguiente (ver el bloque config.preview en startView).
    @Volatile private var previewScope: CoroutineScope? = null

    private val isKaroo = karooSystem.hardwareType == HardwareType.KAROO

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

    // MANTENER: este job es necesario para detectar isOnClimb
    private fun checkClimbStatus(scope: CoroutineScope): Job {
        return karooSystem.streamDataFlow(DataType.Type.ELEVATION_TO_TOP)
            .map { elevationState ->
                ((elevationState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_TO_TOP_ID") ?: 0.0) > 0.0
            }
            .distinctUntilChanged()
            .onEach { newIsOnClimb ->
                isOnClimbFlow.value = newIsOnClimb
                Timber.d("CLIMB isOnClimb changed to: $newIsOnClimb")
            }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)  // usa el scope padre — se cancela automáticamente con él
    }


    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("CLIMB StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")
        val effectiveFieldSize = getEffectiveFieldSize(config.gridSize.second, config.textSize)
        Timber.d("VIEWCONFIG [CLIMB/$dataTypeId]: viewSize=${config.viewSize} gridSize=${config.gridSize} textSize=${config.textSize} effectiveFieldSize=$effectiveFieldSize")

        val scopeJob = Job()
        val scope = CoroutineScope(Dispatchers.IO + scopeJob)
        // setCancellable ignora deliberadamente el cancel cuando config.preview=true (cancelarlo
        // ahí dejaba el editor de perfiles en blanco), así que el scope de un preview no lo
        // cancela NADIE en el acto: el apagado va con margen, en el propio setCancellable
        // (ver Delay.PREVIEW_GRACE abajo). Aquí solo se sustituye un preview por el siguiente.
        // OJO: NO cancelar desde una invocación viva. karoo-ext resuelve la implementación por
        // typeId pero guarda las vistas por id de attachment, así que el editor de perfiles y
        // una vista de ruta del MISMO datatype pueden estar attachados a la vez; cancelar el
        // preview desde la vista viva congelaría el editor que el usuario está mirando.
        if (config.preview) {
            previewScope?.cancel()
            previewScope = scope
        }

        var isAlwaysclimbOnEnabled = true
        var isShowClimbField = true

        // Local a ESTA invocación de startView. `types` en KarooCustomFieldExtension es un
        // `by lazy`, así que existe UN solo objeto por datatype durante toda la vida del
        // proceso: con un campo de instancia, el cancel de una vista anterior — que el SDK
        // puede disparar DESPUÉS de haber arrancado la siguiente — ponía el flag a true y
        // congelaba la vista nueva el resto de la ruta, con todos sus streams vivos.
        val isCancelled = AtomicBoolean(false)
        ViewState.setCancelled(false)

        // En preview NO abrimos el stream real de ELEVATION_TO_TOP: se llamaba antes de mirar
        // config.preview, así que cada visita al editor de perfiles dejaba un consumer Binder
        // vivo para el resto de la sesión. El preview no necesita el estado real de subida.
        if (!config.preview) checkClimbStatus(scope)

        // OPTIMIZACIÓN: reutilizar bitmap solo para el círculo base (no datos)
        val baseBitmap = cachedBaseBitmap
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.circle).also { cachedBaseBitmap = it }

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
            }.distinctUntilChanged()



        val configjob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }

        val viewjob = scope.launch {
            try {

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
                        // Jitter 400-700ms para desincronizar arranques entre fields sin
                        // exponer al usuario a esperas largas viendo "Searching…" (antes 400-1900ms).
                        delay(400L + (Random.nextInt(4) * 100L))

                    }

                    Timber.d("CLIMBStarting view flow: $extension $globalIndex  karooSystem@$karooSystem ")


                    dataflow.flatMapLatest { state ->
                        val (settings, generalSettings, userProfile) = state

                        if (userProfile == null) {
                            Timber.d("CLIMB UserProfile no disponible")
                            // Antes emitía un Triple aquí, pero el onEach hace `result as
                            // ClimbResultData`: si esta rama llegaba a correr, petaba con
                            // ClassCastException en vez de mostrar "Searching". Misma forma
                            // que la rama normal.
                            return@flatMapLatest flowOf(
                                ClimbResultData(
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    StreamState.Searching,
                                    state
                                ) to false
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



                        val headwindFlow =
                            if (listOf(
                                    primaryField,
                                    secondaryField,
                                    thirdField,
                                    fourthField,
                                    climbField,
                                    climbOnField
                                ).any { it.kaction.name == "HEADWIND" } && generalSettings.isheadwindenabled
                            )
                                createHeadwindFlow(karooSystem, refreshTime) else flowOf(
                                StreamHeadWindData(0.0, 0.0)
                            )

                        val firstFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            primaryField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
                        ) else previewFlow()
                        val secondFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            secondaryField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
                        ) else previewFlow()
                        val thirdFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            thirdField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
                        ) else previewFlow()
                        val fourthFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            fourthField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
                        ) else previewFlow()

                        val climbStartFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            climbField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
                        ) else previewFlow()
                        val climbOnFieldFlow = if (!config.preview) karooSystem.getFieldFlow(
                            climbOnField,
                            headwindFlow,
                            generalSettings,
                            isCancelledProvider = { isCancelled.get() }
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
                            climbStartFieldFlow,
                            climbOnFieldFlow
                        ) { fourth: Any, climbStart: Any, climbOn -> Triple(fourth, climbStart, climbOn) }

                        combine(
                            combinedFlow1,
                            combinedFlow2,
                            // isOnClimbFlow es de la INSTANCIA y sobrevive entre startView, y en
                            // preview no se lanza checkClimbStatus que lo corregiría: el editor
                            // heredaría el estado de subida de la última ruta y elegiría
                            // climbOnField o climbField según eso. En preview, valor fijo.
                            if (config.preview) flowOf(false) else isOnClimbFlow
                        ) { triple1, triple2, onClimb ->
                            val (firstState, secondState, thirdState) = triple1
                            val (fourthState, climbStartState,climbOnState) = triple2

                            ClimbResultData(
                                firstState,
                                secondState,
                                thirdState,
                                fourthState,
                                climbStartState,
                                climbOnState,
                                state
                            ) to onClimb
                        }

                    // conflate() descarta emisiones intermedias mientras el render está ocupado.
                    // Sin esto, los 6 streams producen más emisiones de las que se pueden renderizar
                    // (6 streams × 1Hz > 5 renders/seg con delay 200ms) → cola que crece 4-5s de lag.
                    }
                    // `isOnClimb` va DENTRO del combine, no leído aparte desde checkClimbStatus:
                    // fuera de la tupla no solo impedía deduplicar, es que además la transición a
                    // subida no emitía por sí sola — con el ciclista parado al pie del puerto se
                    // quedaba esperando a que emitiera cualquier otro stream. Ya dentro, la tupla
                    // vuelve a ser la entrada completa del render y distinctUntilChanged es
                    // correcto: 6 streams a 1Hz recomponiendo sin que cambie nada era el mayor
                    // desperdicio de CPU de la vista.
                    .distinctUntilChanged()
                    .conflate().onEach { (result, isOnClimb) ->
                        if (isCancelled.get()) {
                            Timber.d("CLIMB Skipping update, job cancelled: $extension $globalIndex")
                            return@onEach
                        }

                        //Timber.d("CLIMB Result: $result")



                        val (firstFieldState, secondFieldState, thirdFieldState,
                            fourthFieldState, climbStartFieldState, climbOnFieldState,globalConfig) = result as ClimbResultData

                        val climbFieldState = if (isOnClimb) climbOnFieldState else climbStartFieldState


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
                            secondField((settings)),
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

                        val (climbvalue, climbIconcolor, climbColorzone, _, climbvalueRight) = getFieldState(
                            climbFieldState,
                            if(isOnClimb) climbOnField(settings) else climbField(settings),
                            context,
                            userProfile,
                            generalSettings.ispalettezwift
                        )



                        val (winddiff, windtext) = listOf(
                            firstFieldState, secondFieldState,
                            thirdFieldState, fourthFieldState,
                            climbStartFieldState, climbOnFieldState
                        ).firstOrNull { it is StreamHeadWindData }
                            ?.let { it as StreamHeadWindData }
                            ?.let { it.diff to convertWindSpeed(it.windSpeed, userProfile.preferredUnit.distance).roundToInt().toString() }
                            ?: (0.0 to "")


                        val clayout = when {
                            generalSettings.iscenterkaroo -> when (config.alignment) {
                                ViewConfig.Alignment.CENTER -> FieldPosition.CENTER
                                ViewConfig.Alignment.LEFT -> FieldPosition.LEFT
                                ViewConfig.Alignment.RIGHT -> FieldPosition.RIGHT
                            }

                            else -> generalSettings.iscenteralign
                        }

                        // En preview isOnClimb ya no se alimenta del stream real, así que el
                        // editor mostraría el layout SIN campo de climb salvo que el usuario
                        // tenga "always on". Forzarlo aquí hace que la vista previa enseñe lo
                        // que el usuario ha configurado, y de forma determinista.
                        isShowClimbField  = (isOnClimb || isAlwaysclimbOnEnabled || config.preview)

                        val isfirsthorizontal = if (isShowClimbField) false else isfirsthorizontal(settings)
                        val issecondhorizontal = if (isShowClimbField) false else issecondhorizontal(settings)


                       // Timber.d("isfirsthorizontal: $isfirsthorizontal, issecondhorizontal: $issecondhorizontal")

                        //Timber.w("CLIMB field climbField: ${climbField(settings)}  isOnClimb: $isOnClimb isAlwaysclimbOnEnabled: $isAlwaysclimbOnEnabled")
                        try {
                            if (isCancelled.get()) {
                                Timber.d("CLIMB Skipping composition, job cancelled: $extension $globalIndex")
                                return@onEach
                            }
                            withContext(Dispatchers.Main) {
                                if (isCancelled.get()) return@withContext
                                val newView = glance.compose(context, DpSize.Unspecified) {
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
                                         if(isOnClimb) climbOnField(settings) else climbField(settings),
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
                                         effectiveFieldSize,
                                        isKaroo,
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
                                        isShowClimbField,
                                        generalSettings.distanceWithDecimals,
                                        firstFieldState = firstFieldState as? StreamState,
                                        secondFieldState = secondFieldState as? StreamState,
                                        thirdFieldState = thirdFieldState as? StreamState,
                                        fourthFieldState = fourthFieldState as? StreamState,
                                        climbFieldState = climbFieldState as? StreamState,
                                    )
                                }.remoteViews
                                // Timber.d("CLIMB Updating view: $extension $globalIndex values: $firstvalue, $secondvalue layout: $clayout")
                                if (!isCancelled.get()) emitter.updateView(newView)
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

                            when {

                                cause is CancellationException && isCancelled.get() -> {
                                    Timber.d("CLIMB  No se reintenta el flujo cancelado por el emitter: $extension $globalIndex")
                                    false  // Importante: no reintentar
                                }

                                attempt > 4 -> {
                                    Timber.e(
                                        cause,
                                        "CLIMB Max retries reached: $extension $globalIndex (attempt $attempt) "
                                    )

                                    delay(Delay.RETRY_LONG.time)
                                    //startView(context, config, emitter)
                                    true
                                } else ->{
                                    Timber.w(
                                        cause,
                                        "CLIMB Retrying flow: $extension $globalIndex (attempt $attempt) "
                                    )
                                    delay(Delay.RETRY_SHORT.time)
                                    true
                                }
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
                // Si esta vista está en modo preview (p.ej. Profile del Karoo), no hacemos la cancelación completa
                Timber.d("CANCEL CLIMB and config.preview is = %s", config.preview)
                if (config.preview) {
                    Timber.w("Emitter.setCancellable ignored because config.preview=true (profile/preview). extension=$extension index=$globalIndex")
                    // Cancelar el scope aquí mismo dejaba el editor de perfiles en blanco, así que no se
                    // cancela en el acto — pero tampoco puede no cancelarse nunca: así quedaba un previewFlow
                    // por datatype emitiendo cada 2s y componiendo Glance contra un emitter muerto durante el
                    // resto de la sesión. Se apaga con margen: si el editor sigue vivo volverá a llamar a
                    // startView y ese preview nuevo sustituye a este antes de que expire la gracia.
                    scope.launch {
                        delay(Delay.PREVIEW_GRACE.time)
                        Timber.d("Preview scope self-cancel tras gracia: $extension $globalIndex")
                        scope.cancel()
                    }
                    return@setCancellable
                }


                Timber.d("Cancelando todos los jobs y flujos de CLIMB")
                isCancelled.set(true)
                ViewState.setCancelled(true)

                configjob.cancel()
                viewjob.cancel()
                scope.cancel()
                scopeJob.cancel()

                Timber.d("Cancelación de ClimbTypeBase completada")

            } catch (_: CancellationException) {

            } catch (e: Exception) {
                Timber.e(e, "Error durante la cancelación de CLIMB")
            }

        }
    }
}