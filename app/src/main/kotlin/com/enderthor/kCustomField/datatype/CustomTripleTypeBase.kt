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
import kotlinx.coroutines.flow.distinctUntilChanged

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

import com.enderthor.kCustomField.extensions.streamTripleFieldSettings
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

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomTripleTypeBase(
    private val karooSystem: KarooSystemService,
    datatype: String,
    private val globalIndex: Int
) : DataTypeImpl("kcustomfield", datatype) {


    private val glance = GlanceRemoteViews()
    private val firstField = { settings: TripleFieldSettings -> settings.onefield }
    private val secondField = { settings: TripleFieldSettings -> settings.secondfield }
    private val thirdField = { settings: TripleFieldSettings -> settings.thirdfield }

    private val isKaroo = karooSystem.hardwareType == HardwareType.KAROO

    private val refreshTime: Long
        get() = when (karooSystem.hardwareType) {
            HardwareType.K2 -> RefreshTime.MID.time
            else -> RefreshTime.HALF.time
        }.coerceAtLeast(100L)

    // Decodificado una vez por instancia: startView() se re-entra muy rápido en cambios
    // de página/perfil y re-decodificar el recurso en cada entrada es trabajo inútil.
    @Volatile private var cachedBaseBitmap: Bitmap? = null
    // Scope del último preview servido por esta instancia, para poder cancelarlo cuando llega
    // el siguiente (ver el bloque config.preview en startView).
    @Volatile private var previewScope: CoroutineScope? = null



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
        Timber.d("TRIPLE StartView: field $extension index $globalIndex field $dataTypeId config: $config emitter: $emitter")
        val effectiveFieldSize = getEffectiveFieldSize(config.gridSize.second, config.textSize)
        Timber.d("VIEWCONFIG [TRIPLE/$dataTypeId]: viewSize=${config.viewSize} gridSize=${config.gridSize} textSize=${config.textSize} effectiveFieldSize=$effectiveFieldSize")

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
        // Local a ESTA invocación de startView. `types` en KarooCustomFieldExtension es un
        // `by lazy`, así que existe UN solo objeto por datatype durante toda la vida del
        // proceso: con un campo de instancia, el cancel de una vista anterior — que el SDK
        // puede disparar DESPUÉS de haber arrancado la siguiente — ponía el flag a true y
        // congelaba la vista nueva el resto de la ruta, con todos sus streams vivos.
        val isCancelled = AtomicBoolean(false)
        ViewState.setCancelled(false)

        val dataflow = context.streamTripleFieldSettings()
            .onStart {
                Timber.d("Iniciando streamTripleFieldSettings")
                emit(previewTripleFieldSettings)
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
                TripleGlobalConfigState(settings, generalSettings, userProfile)
            }.distinctUntilChanged()



        val configjob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }

        val baseBitmap = cachedBaseBitmap
            ?: BitmapFactory.decodeResource(context.resources, R.drawable.circle).also { cachedBaseBitmap = it }
        val viewjob = scope.launch {
            try {
                Timber.d("TRIPLE Starting view: $extension $globalIndex ")

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
                            Timber.e(e, "TRIPLE Error en vista inicial: $extension $globalIndex ")
                        }
                        // Jitter 400-700ms para desincronizar arranques entre fields sin
                        // exponer al usuario a esperas largas viendo "Searching…" (antes 400-1900ms).
                        delay(400L + (Random.nextInt(4) * 100L))

                    }

                    Timber.d("TRIPLE Starting view flow: $extension $globalIndex  karooSystem@$karooSystem ")

                    dataflow
                        .flatMapLatest { state ->
                            val (settings, generalSettings, _) = state

                            val currentSettings = settings.getOrNull(globalIndex)
                                ?: throw IndexOutOfBoundsException("Invalid index $globalIndex")

                            val primaryField = firstField(currentSettings)
                            val secondaryField = secondField(currentSettings)
                            val tertiaryField = thirdField(currentSettings)

                            val headwindFlow =
                                if (listOf(
                                        primaryField, secondaryField, tertiaryField
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
                                tertiaryField,
                                headwindFlow,
                                generalSettings,
                                isCancelledProvider = { isCancelled.get() }
                            ) else previewFlow()

                            combine(
                                firstFieldFlow,
                                secondFieldFlow,
                                thirdFieldFlow
                            ) { first: Any, second: Any, third: Any ->
                                TripleResultData(first, second, third, state)
                            }
                        }
                        // La vista es función pura de los 3 estados más la config: si la tupla
                        // repite, la composición Glance y el updateView por Binder que vendrían
                        // detrás son trabajo tirado. StreamState/DataPoint son data class.
                        .distinctUntilChanged()
                        .conflate()
                        .onEach { result ->

                        if (isCancelled.get()) {
                            Timber.d("TRIPLE Skipping update, job cancelled: $extension $globalIndex")
                            return@onEach
                        }
                        val (firstFieldState, secondFieldState, thirdFieldState, globalConfig) = result

                        val setting = globalConfig.settings
                        val generalSettings = globalConfig.generalSettings
                        val userProfile = globalConfig.userProfile

                        if (userProfile == null) {
                                Timber.d("UserProfile no disponible")
                                return@onEach
                            }
                            val settings = setting[globalIndex]

                            val rawStates = listOf(firstFieldState, secondFieldState, thirdFieldState)
                            val fields = listOf(firstField(settings), secondField(settings), thirdField(settings))
                            val fieldStates = rawStates.zip(fields).map { (fieldState, field) ->
                                getFieldState(fieldState, field, context, userProfile, generalSettings.ispalettezwift)
                            }
                            val cells = fieldStates.mapIndexed { i, fs ->
                                val (value, iconColor, zoneColor, _, valueRight) = fs
                                TripleCell(value, valueRight, fields[i], iconColor, zoneColor, rawStates[i] as? StreamState)
                            }

                            val (winddiff, windtext) = listOf(
                                firstFieldState, secondFieldState, thirdFieldState
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
                                settings.ishorizontal -> generalSettings.iscenteralign
                                else -> generalSettings.iscentervertical
                            }

                            // Ocultar celdas sin datos: solo con el switch y nunca en preview. Igual que
                            // en Double: RollingFieldScreen solo soporta SMALL/MEDIUM, así que en
                            // tamaños mayores se mantienen al menos 2 celdas.
                            val minVisible = if (effectiveFieldSize == FieldSize.SMALL || effectiveFieldSize == FieldSize.MEDIUM) 1 else 2
                            val visible = visibleIndices(
                                settings.hideEmpty && !config.preview,
                                rawStates.mapIndexed { i, s -> isEmptyMetric(s as? StreamState, fields[i].kaction) },
                                minVisible
                            )

                            try {
                                if (isCancelled.get()) {
                                    Timber.d("TRIPLE Skipping composition, job cancelled: $extension $globalIndex")
                                    return@onEach
                                }
                                withContext(Dispatchers.Main) {
                                    if (isCancelled.get()) return@withContext
                                    val newView = glance.compose(context, DpSize.Unspecified) {
                                        if (visible.size == 1) {
                                            val i = visible[0]
                                            val kaction = fields[i].kaction
                                            val rawState = rawStates[i]
                                            val cell = cells[i]
                                            val (_, _, _, isRealZone) = fieldStates[i]
                                            RollingFieldScreen(
                                                cell.value,
                                                isIntField(kaction, false, false, generalSettings.distanceWithDecimals),
                                                kaction,
                                                cell.iconColor,
                                                cell.zoneColor,
                                                effectiveFieldSize,
                                                isKaroo,
                                                clayout,
                                                windtext,
                                                winddiff.roundToInt(),
                                                baseBitmap,
                                                rawState is StreamState,
                                                config.textSize,
                                                isRealZone,
                                                config.preview,
                                                cell.valueRight,
                                                fieldState = rawState as? StreamState
                                            )
                                        } else TripleScreenSelector(
                                            visible.map { cells[it] },
                                            settings.ishorizontal,
                                            effectiveFieldSize,
                                            isKaroo,
                                            clayout,
                                            generalSettings.isdivider,
                                            generalSettings.distanceWithDecimals,
                                            windtext,
                                            winddiff.roundToInt(),
                                            baseBitmap
                                        )
                                    }.remoteViews
                                    if (!isCancelled.get()) emitter.updateView(newView)
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) {
                                    Timber.d("TRIPLE View update cancelled normally: $extension $globalIndex")
                                } else {
                                    Timber.e(e, "TRIPLE Error composing/updating view: $extension $globalIndex")
                                    if (coroutineContext.isActive && !isCancelled.get()) {
                                        throw e
                                    }
                                }
                            }

                            delay(refreshTime)
                        }
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    Timber.d("TRIPLE Flow cancelled: $extension $globalIndex")
                                    throw e
                                }
                                else -> {
                                    Timber.e(e, "TRIPLE Flow error: $extension $globalIndex")
                                    throw e
                                }
                            }
                        }
                        .retryWhen { cause, attempt ->

                            when {

                                cause is CancellationException && isCancelled.get() -> {
                                    Timber.d("TRIPLE No se reintenta el flujo cancelado por el emitter: $extension $globalIndex")
                                    false
                                }
                                attempt > 4 -> {
                                    Timber.e(cause, "TRIPLE Max retries reached: $extension $globalIndex (attempt $attempt)")
                                    delay(Delay.RETRY_LONG.time)
                                    true
                                }
                                else -> {
                                    Timber.w(cause, "TRIPLE Retrying flow: $extension $globalIndex (attempt $attempt)")
                                    delay(Delay.RETRY_SHORT.time)
                                    true
                                }
                            }

                        }
                        .launchIn(scope)

                } catch (e: CancellationException) {
                    Timber.d("TRIPLE View operation cancelled: $extension $globalIndex ")
                    throw e
                }
                catch (e: DeadObjectException) {
                    Timber.e(e, "TRIPLE Dead object en vista principal, parando")
                    scope.cancel()
                }

          } catch (e: Exception) {
                Timber.e(e, "TRIPLE ViewJob error: $extension $globalIndex ")
                if (!scope.isActive) return@launch
                delay(1000L)

            }
        }

        emitter.setCancellable {
            try {
                if (config.preview) {
                    // Cancelar el scope aquí mismo dejaba el editor de perfiles en blanco, así
                    // que no se cancela en el acto — pero tampoco puede no cancelarse nunca: así
                    // quedaba un previewFlow por datatype emitiendo cada 2s y componiendo Glance
                    // contra un emitter muerto durante el resto de la sesión. Se apaga con
                    // margen: si el editor sigue vivo volverá a llamar a startView y ese preview
                    // nuevo sustituye a este antes de que expire la gracia.
                    scope.launch {
                        delay(Delay.PREVIEW_GRACE.time)
                        Timber.d("Preview scope self-cancel tras gracia: $extension $globalIndex")
                        scope.cancel()
                    }
                    return@setCancellable
                }

                Timber.d("TRIPLE Emitter.setCancellable: extension=$extension index=$globalIndex")

                isCancelled.set(true)
                ViewState.setCancelled(true)
                configjob.cancel()
                viewjob.cancel()
                scope.cancel()
                scopeJob.cancel()

            } catch (_: CancellationException) {
                // normal
            } catch (e: Exception) {
                Timber.e(e, "TRIPLE Error durante la cancelación: $extension $globalIndex")
            }

        }
    }
}
