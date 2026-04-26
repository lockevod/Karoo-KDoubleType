package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.FlowPreview
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import com.enderthor.kCustomField.extensions.wprimeZones
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.streamUserProfile
import com.enderthor.kCustomField.extensions.streamWPrimeBalanceSettings
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber


import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.isActive

import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.exp



class StickyStreamState private constructor() {
    companion object {
        private const val MAX_STATES = 20
        private val lastValidStates = object : LinkedHashMap<String, Pair<Any, Long>>(MAX_STATES, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Pair<Any, Long>>) = size > MAX_STATES
        }
        private const val STICKY_TIMEOUT_MS = 7000L

        fun process(state: Any, actionName: String): Any {
            val currentTime = System.currentTimeMillis()


            if (state is StreamState.Streaming) {
                lastValidStates[actionName] = Pair(state, currentTime)
                return state
            }

            val lastStatePair = lastValidStates[actionName] ?: return state
            val (lastState, timestamp) = lastStatePair

            if (currentTime - timestamp < STICKY_TIMEOUT_MS) {
                return lastState
            }

            return state
        }
    }
}


internal object ViewState {
    @Volatile
    private var _isCancelled = false

    val isCancelledByEmitter: Boolean
        get() = _isCancelled

    fun isCancelled(): Boolean = _isCancelled

    @Synchronized
    fun setCancelled(value: Boolean) {
        _isCancelled = value
        Timber.d("ViewState.cancelled = $value")
    }
}

fun getColorZone(
    context: Context,
    zone: String,
    value: Double,
    userProfile: UserProfile,
    isPaletteZwift: Boolean
): ColorProvider {


    val zoneData = when (zone) {
        "heartRateZones" -> userProfile.heartRateZones
        "powerZones" -> userProfile.powerZones
        "wprimeZones" -> wprimeZones
        else -> slopeZones
    }

    val colorResource = getZone(zoneData, value)?.let {
        if (isPaletteZwift) it.colorZwift else it.colorResource
    } ?: R.color.zone7

    return ColorProvider(
        day = Color(ContextCompat.getColor(context, colorResource)),
        night = Color(ContextCompat.getColor(context, colorResource))
    )
}


fun convertValue(
    streamState: StreamState,
    convert: String,
    unitType: UserProfile.PreferredUnit.UnitType,
    type: String
): Double {
    //Timber.d("convertValue: convert=$convert, unitType=$unitType, type=$type, streamState=$streamState")
    val value = when (type) {
        "TYPE_ELEVATION_REMAINING_ID" -> (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ASCENT_REMAINING_ID")
            ?: (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_REMAINING_ID")
        "TYPE_DISTANCE_TO_DESTINATION_ID" -> (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_DISTANCE_TO_DESTINATION_ID")
        "TYPE_VERTICAL_SPEED_ID", "TYPE_AVERAGE_VERTICAL_SPEED_30S_ID" ->
            (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_VERTICAL_SPEED_ID")
        "SHIFTING_FRONT_GEAR" -> (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_SHIFTING_FRONT_GEAR_ID")
        "SHIFTING_REAR_GEAR" -> (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_SHIFTING_FRONT_REAR_ID")
        "TIRE_PRESSURE_FRONT","TIRE_PRESSURE_REAR" -> ((streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_TIRE_PRESSURE_ID"))
        "TYPE_ELEVATION_TO_TOP" -> ((streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_TO_TOP_ID"))
        "TYPE_ELEVATION_FROM_BOTTOM" -> ((streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_FROM_BOTTOM_ID"))
        else -> (streamState as? StreamState.Streaming)?.dataPoint?.singleValue
    } ?: 0.0

    val convertedValue = when (convert) {
        "distance", "speed" -> when (unitType) {
            UserProfile.PreferredUnit.UnitType.METRIC ->
                if (convert == "distance") value / 1000 else value * 3.6
            UserProfile.PreferredUnit.UnitType.IMPERIAL ->
                if (convert == "distance") value / 1609.345 else value * 2.237
        }
        "pressure" -> when (unitType) {
            UserProfile.PreferredUnit.UnitType.METRIC -> value / 100.0
            UserProfile.PreferredUnit.UnitType.IMPERIAL -> value * 0.145038
        }
        "elevation" -> when (unitType) {
            // stream proporciona elevación en metros
            UserProfile.PreferredUnit.UnitType.METRIC -> value
            UserProfile.PreferredUnit.UnitType.IMPERIAL -> value * 3.2808399 // m -> ft
        }
        else -> value
    }

    return convertedValue
}

/**
 * Formatea valores de Flight Attendant (FA) a strings legibles
 * Convierte valores numéricos enum a etiquetas descriptivas
 */
fun formatFAValue(fieldState: StreamState, actionName: String): String {
    if (fieldState !is StreamState.Streaming) return "--"
    
    val value = fieldState.dataPoint.singleValue?.toInt() ?: 0
    
    return when (actionName) {
        // Estados de suspensión (enum values de Flight Attendant)
        "FA_SUSPENSION_STATE_FRONT", "FA_SUSPENSION_STATE_REAR" -> {
            when (value) {
                0 -> "Close"
                1 -> "Open"
                2 -> "Lock"
                3 -> "Med"
                4 -> "Firm"
                5 -> "Soft"
                else -> "NA"
            }
        }
        // Conteos de compresión (mostrar número simples)
        "FA_SUSPENSION_STATE_COUNT_FRONT", "FA_SUSPENSION_STATE_COUNT_REAR" -> {
            value.toString()
        }
        // Sesgo/balance de suspensión (mostrar con porcentaje)
        "FA_SUSPENSION_BIAS" -> {
            "$value%"
        }
        else -> value.toString()
    }
}

/**
 * Verifica si un valor de FA está disponible
 */
fun isFAFieldAvailable(fieldState: StreamState): Boolean {
    return fieldState is StreamState.Streaming && fieldState.dataPoint.singleValue != null
}

/**
 * Obtiene el valor numérico de un campo FA para usar en colores/zonas
 */
fun getFANumericValue(fieldState: StreamState): Double {
    return (fieldState as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0
}

fun getColorProvider(context: Context, action: KarooAction, colorzone: Boolean): ColorProvider {


    return (if (colorzone) {
        ColorProvider(Color.Black, Color.Black)
    } else {
        ColorProvider(
            day = Color(ContextCompat.getColor(context, action.colorday)),
            night = Color(ContextCompat.getColor(context, action.colornight))
        )
    })
}

fun getFieldSize(size: Int): FieldSize {
    return fieldSizeRanges.first { size in it.min..it.max }.name
}

/**
 * Estima FieldSize a partir del textSize que proporciona el sistema Karoo.
 * El textSize ya está calibrado por el sistema en función del espacio real del slot,
 * por lo que es más fiable que gridSize para detectar slots reducidos (ej. pantalla climber).
 *
 * Umbrales conservadores: para calibración precisa, consultar logs VIEWCONFIG
 * con build debug y `adb logcat | grep VIEWCONFIG` en cada pantalla.
 */
fun getFieldSizeFromTextSize(textSize: Int): FieldSize = when {
    textSize >= 33 -> FieldSize.EXTRA_LARGE
    textSize >= 25 -> FieldSize.LARGE
    textSize >= 18 -> FieldSize.MEDIUM
    else -> FieldSize.SMALL
}

/**
 * Devuelve el FieldSize efectivo usando el mínimo (más conservador) entre
 * la estimación por gridSize y la estimación por textSize.
 * En pantallas normales ambas coinciden. En slots reducidos (ej. pantalla climber del GPS)
 * donde textSize es menor, automáticamente se usa un tamaño más compacto.
 */
fun getEffectiveFieldSize(gridSizeSecond: Int, textSize: Int): FieldSize {
    val sizeFromGrid = getFieldSize(gridSizeSecond)
    val sizeFromText = getFieldSizeFromTextSize(textSize)
    return if (sizeFromText.ordinal < sizeFromGrid.ordinal) sizeFromText else sizeFromGrid
}

/**
 * Convierte velocidad del viento de m/s (unidad nativa del stream karoo-headwind)
 * a la unidad preferida del usuario.
 * Factores idénticos a los de la extensión karoo-headwind (Conversion.kt):
 *  - METRIC:   m/s × 3.6             → km/h
 *  - IMPERIAL: m/s × 2.2369362920544 → mph
 */
fun convertWindSpeed(speedMs: Double, unitType: UserProfile.PreferredUnit.UnitType): Double =
    when (unitType) {
        UserProfile.PreferredUnit.UnitType.METRIC -> speedMs * 3.6
        UserProfile.PreferredUnit.UnitType.IMPERIAL -> speedMs * 2.2369362920544
    }

@OptIn(FlowPreview::class)
fun createHeadwindFlow(
    karooSystem: KarooSystemService,
    period: Long
): Flow<StreamHeadWindData> = flow {
    combine(
        karooSystem.streamDataFlow(Headwind.DIFF.type)
            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue },
        karooSystem.streamDataFlow(Headwind.SPEED.type)
            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
    ) { diff, speed ->
        StreamHeadWindData(diff, speed)
    }
        .onStart { emit(StreamHeadWindData(0.0, 0.0)) }
        .distinctUntilChanged()
        .debounce(200 + period)
        .conflate()
        .catch { e ->
            Timber.e(e, "Error in headwindFlow")
            emit(StreamHeadWindData(0.0, 0.0))
        }
        .collect { emit(it) }
}

@OptIn(FlowPreview::class)
fun KarooSystemService.getFieldFlow(
    field: Any,
    headwindFlow: Flow<StreamHeadWindData>,
    generalSettings: GeneralSettings,
    context: Context? = null
): Flow<Any> = flow {


    try {
        val (actionId, action) = when (field) {
            is DoubleFieldType -> field.kaction.action to field.kaction
            is OneFieldType -> field.kaction.action to field.kaction
            else -> throw IllegalArgumentException("Tipo de campo no soportado")
        }

        // Emitir un valor inicial para que combine() no bloquee esperando el primer valor de cada stream.
        // Crítico en ClimbType que combina 6 streams: sin esto, el primer render puede tardar 4-5s
        // si algún stream (p.ej. DISTANCE_TO_TOP fuera de subida) tarda en responder.
        if (action.name == "HEADWIND" && generalSettings.isheadwindenabled) {
            emit(StreamHeadWindData(0.0, 0.0))
        } else {
            emit(StreamState.Searching)
        }

        while (currentCoroutineContext().isActive) {
            try {
                val streamFlow = when {
                    action.name == "HEADWIND" && generalSettings.isheadwindenabled -> {
                        headwindFlow
                            .catch { e ->
                                when (e) {
                                    is CancellationException -> {
                                        if (ViewState.isCancelledByEmitter) throw e
                                        // OPTIMIZACIÓN: menos logging para reducir overhead
                                    }
                                    else -> {
                                        Timber.e(e, "Error en headwindFlow")
                                        emit(StreamHeadWindData(0.0, 0.0))
                                    }
                                }
                            }
                            .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    action.name == "VO2MAX" -> {
                        streamDataFlow(DataType.Type.NORMALIZED_POWER)
                            .combine(streamUserProfile()) { powerState, profile ->
                                if (powerState is StreamState.Streaming) {
                                    val powerValue = powerState.dataPoint.singleValue ?: 0.0
                                    val vo2max = calculateVO2max(powerValue, profile)
                                    StreamState.Streaming(
                                        DataPoint(
                                            dataTypeId = "VO2MAX",
                                            values = mapOf(DataType.Field.SINGLE to vo2max)
                                        )
                                    )
                                } else {
                                    StreamState.Searching
                                }
                            }
                            .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    action.name == "GEARS" -> {
                        streamDataFlow(DataType.Type.SHIFTING_GEARS)
                            .map { gearState ->
                                if (gearState is StreamState.Streaming) {

                                    // raw object that comes from the extension SDK
                                    val raw = gearState.dataPoint.values

                                    // 1) type and raw toString
                                    //Timber.d("SHIFTING_GEARS raw type = %s", raw?.let { it::class.java.name } ?: "null")
                                    //Timber.d("SHIFTING_GEARS raw value = %s", raw)

                                    val map = raw as? Map<String, *>

                                    val frontGear    = map?.get("FIELD_SHIFTING_FRONT_GEAR_ID") as? Double ?: 0.0
                                    //val frontMax = map?.get("FIELD_SHIFTING_FRONT_GEAR_MAX_ID") as? Double ?: 0.0
                                    val rearGear     = map?.get("FIELD_SHIFTING_REAR_GEAR_ID") as? Double ?: 0.0
                                    //val rearMax  = map?.get("FIELD_SHIFTING_REAR_GEAR_MAX_ID") as? Double ?: 0.0


                                    //Timber.d("Parsed gears: F=%s/%s R=%s/%s", frontGear, frontMax, rearGear, rearMax)

                                    val combinedGear = frontGear *100 + rearGear
                                    //Timber.d("CombindesGear: %s", combinedGear)

                                    StreamState.Streaming(
                                        DataPoint(
                                            dataTypeId = "GEARS",
                                            values = mapOf(DataType.Field.SINGLE to combinedGear)
                                        )
                                    )
                                } else {
                                    StreamState.Searching
                                }
                            }
                            .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    action.name == "FTP" -> {
                        streamDataFlow(DataType.Type.NORMALIZED_POWER)
                            .map { powerState ->
                                if (powerState is StreamState.Streaming) {
                                    val powerValue = powerState.dataPoint.singleValue ?: 0.0
                                    val FTP = calculateFTP(powerValue)
                                    StreamState.Streaming(
                                        DataPoint(
                                            dataTypeId = "FTP",
                                            values = mapOf(DataType.Field.SINGLE to FTP)
                                        )
                                    )
                                } else {
                                    StreamState.Searching
                                }
                            }
                            .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    action.name == "FTPG" -> {
                        streamDataFlow(DataType.Type.NORMALIZED_POWER)
                            .combine(streamUserProfile()) { powerState, profile ->
                                if (powerState is StreamState.Streaming) {
                                    val powerValue = powerState.dataPoint.singleValue ?: 0.0
                                    val FTPG = calculateFTPG(powerValue, profile)
                                    StreamState.Streaming(
                                        DataPoint(
                                            dataTypeId = "FTPG",
                                            values = mapOf(DataType.Field.SINGLE to FTPG)
                                        )
                                    )
                                } else {
                                    StreamState.Searching
                                }
                            }
                            .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    action.name == "WPRIME_BALANCE" -> {
                        combine(
                            streamDataFlow(DataType.Type.POWER),
                            streamUserProfile(),
                            context?.streamWPrimeBalanceSettings() ?: flowOf(WPrimeBalanceSettings())
                        ) { powerState, profile, wPrimeSettings ->
                            if (powerState is StreamState.Streaming) {
                                val powerValue = powerState.dataPoint.singleValue ?: 0.0
                                val wPrimeBalance = calculateWPrimeBalance(powerValue, profile, wPrimeSettings)
                                StreamState.Streaming(
                                    DataPoint(
                                        dataTypeId = "WPRIME_BALANCE",
                                        values = mapOf(DataType.Field.SINGLE to wPrimeBalance)
                                    )
                                )
                            } else {
                                StreamState.Searching
                            }
                        }
                        .distinctUntilChanged() // OPTIMIZACIÓN: evitar updates innecesarios
                        .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    else -> streamDataFlow(action.action)
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    if (ViewState.isCancelledByEmitter) throw e
                                }
                                else -> {
                                    Timber.e(e, "Error en streamDataFlow")
                                    emit(StreamState.NotAvailable)
                                }
                            }
                        }
                        .timeout(STREAM_TIMEOUT.milliseconds)
                }

                // OPTIMIZACIÓN: aplicar distinctUntilChanged antes del collect
                streamFlow.distinctUntilChanged().collect { state ->
                    val processedState = StickyStreamState.process(state, action.name)
                    emit(processedState)
                }

            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> {
                        if (ViewState.isCancelledByEmitter) {
                            Timber.d("getFieldFlow cancelado por emitter para ${action.name}")
                            throw e
                        }
                        Timber.d("Cancelación ignorada en getFieldFlow para ${action.name}")
                        delay(WAIT_STREAMS_SHORT)
                    }
                    else -> {
                        Timber.e(e, "Error en getFieldFlow para ${action.name}")
                        emit(StreamState.NotAvailable)
                        delay(WAIT_STREAMS_SHORT)
                    }
                }
            }
        }
    } catch (e: CancellationException) {
        if (ViewState.isCancelledByEmitter) {
            Timber.d("getFieldFlow cancelado por emitter")
            throw e
        }
        Timber.d("Cancelación ignorada en getFieldFlow")
    }
}.retryWhen { cause, attempt ->
        when {
            cause is CancellationException && !ViewState.isCancelledByEmitter -> true
            attempt > RETRY_CHECK_STREAMS -> {
                Timber.e("Máximo de reintentos alcanzado")
                emit(StreamState.Idle)
                delay(WAIT_STREAMS_NORMAL)
                true
            }

            else -> {
                Timber.w("Reintentando stream, intento $attempt")
                //emit(StreamState.Idle)
                delay(WAIT_STREAMS_SHORT)
                true
            }
        }
    }
    .catch { e ->
        when {
            e is CancellationException && ViewState.isCancelledByEmitter -> throw e
            else -> {
                Timber.e(e, "Error fatal en getFieldFlow")
                emit(StreamState.NotAvailable)
            }
        }
    }

fun calculateVO2max(powerValue: Double, userProfile: UserProfile): Double {
    val weight = userProfile.weight
    val wattsPerKg = powerValue / weight

    //  Hawley & Noakes
    return 10.8 * wattsPerKg + 7.0
}

fun calculateFTP(powerValue: Double): Double {
    return powerValue * 0.95
}


fun calculateFTPG(powerValue: Double, userProfile: UserProfile): Double {
    // 0 si no hay potencia
    if (powerValue <= 0.0) return 0.0

    val weight = userProfile.weight
    // Si no hay peso válido, aplicar la regla básica del 95 %
    if (weight <= 0) return powerValue * 0.95

    // Vatios por kilo
    val wattsPerKg = powerValue / weight

    // Seleccionamos el factor según la referencia Allen & Coggan (más simple y barato)
    val factor = when {
        wattsPerKg >= 4.0 -> 0.97
        wattsPerKg >= 3.0 -> 0.95
        wattsPerKg >= 2.0 -> 0.93
        else -> 0.90
    }

    // Estimación preliminar del FTP a partir de la potencia normalizada
    var ftpEst = powerValue * factor

    // Si el usuario tiene un FTP histórico en las zonas de potencia, ajustamos a ±5 %
    userProfile.powerZones.takeIf { it.size > 3 }?.let { zones ->
        val currentFTP = zones[3].max
        if (currentFTP > 0) {
            val minFTP = currentFTP * 0.95
            val maxFTP = currentFTP * 1.05
            ftpEst = ftpEst.coerceIn(minFTP, maxFTP)
        }
    }

    return ftpEst
}


fun multipleStreamValues(state: StreamState, kaction: KarooAction): Pair<Double, Double> {
    if (state !is StreamState.Streaming) return Pair(0.0, 0.0)

    val (leftDatatype, rightDatatype, onlyfirst) = getMultiFieldsByAction(kaction)
        ?: return Pair(0.0, 0.0)

    val left = state.dataPoint.values[leftDatatype] ?: 0.0
    val right = if (onlyfirst) 1 - left else state.dataPoint.values[rightDatatype] ?: 0.0

    return when {
        left + right != 100.0 && left > 0 -> Pair(left, 100.0 - left)
        left + right != 100.0 && right > 0 -> Pair(100.0 - right, right)
        left + right != 100.0 -> Pair(100.0, 0.0)
        else -> Pair(left, right)
    }
}

fun updateFieldState(
    fieldState: StreamState,
    fieldSettings: Any,
    context: Context,
    userProfile: UserProfile,
    isPaletteZwift: Boolean,
    wPrimeSettings: WPrimeBalanceSettings? = null
): Quintuple<Double, ColorProvider, ColorProvider, Boolean, Double> {
    val (kaction, iszone) = when (fieldSettings) {
        is DoubleFieldType -> fieldSettings.kaction to fieldSettings.iszone
        is OneFieldType -> fieldSettings.kaction to fieldSettings.iszone
        else -> throw IllegalArgumentException("Unsupported field type")
    }

    //Timber.d("updateFieldState: fieldSettings: $fieldSettings, kaction: ${kaction.name}, iszone: $iszone")
    val (value, valueRight) = if (kaction.powerField) {
        multipleStreamValues(fieldState, kaction)
    } else {
        // aquí se llama convertValue con kaction.convert y la unidad del usuario
        Pair(
            convertValue(fieldState, kaction.convert, userProfile.preferredUnit.distance, kaction.action),
            0.0
        )
    }

    val isRealZone = if(checkRealZone(kaction,iszone,value,valueRight)) iszone else false

    val iconColor = getColorProvider(context, kaction, isRealZone)

    // Verificar si es W' Balance y si las zonas visuales están habilitadas
    val shouldUseWPrimeZones = kaction.name == "WPRIME_BALANCE" &&
                              wPrimeSettings?.useVisualZones == true &&
                              iszone

    val colorZone = if ((kaction.zone in listOf("heartRateZones", "powerZones", "slopeZones")) && iszone) {
        getColorZone(context, kaction.zone, value, userProfile, isPaletteZwift)
    } else if (shouldUseWPrimeZones) {
        getColorZone(context, kaction.zone, value, userProfile, isPaletteZwift)
    } else if( (kaction.name =="AVERAGE_PEDAL_BALANCE" || kaction.name =="PEDAL_BALANCE") && iszone && kaction.powerField)
    {
        if (value > valueRight*1.15)
            ColorProvider(
                day = Color(ContextCompat.getColor(context,R.color.zone7switft)),
                night = Color(ContextCompat.getColor(context, R.color.zone7switft))
            )
        else if (value > valueRight*1.07)
            ColorProvider(
                day = Color(ContextCompat.getColor(context,R.color.zone5)),
                night = Color(ContextCompat.getColor(context, R.color.zone5))
            )
        else if (value < valueRight*0.85)
            ColorProvider(
                day = Color(ContextCompat.getColor(context,R.color.zone0)),
                night = Color(ContextCompat.getColor(context, R.color.zone0))
            )
        else if (value < valueRight*0.93)
            ColorProvider(
                day = Color(ContextCompat.getColor(context,R.color.zone9)),
                night = Color(ContextCompat.getColor(context, R.color.zone9))
            )
        else
            ColorProvider(Color.White, Color.Black)
    }  else {
        ColorProvider(Color.White, Color.Black)
    }

    return Quintuple(value, iconColor, colorZone, iszone, valueRight)
}

fun getFieldState(
    fieldState: Any?,
    field: Any,
    context: Context,
    userProfile: UserProfile,
    isPaletteZwift: Boolean
): Quintuple<Double, ColorProvider, ColorProvider, Boolean, Double> {
    return if (fieldState is StreamState) {
        updateFieldState(fieldState, field, context, userProfile, isPaletteZwift)
    } else {
        Quintuple(0.0, ColorProvider(Color.White, Color.Black), ColorProvider(Color.White, Color.Black), false, 0.0)
    }
}


private object WPrimeBalanceState {
    var currentWPrimeBalance = 0.0
    var lastUpdateTime = 0L
    var isInitialized = false
    var smoothedPower = 0.0
    var lastCp = Double.NaN
    var lastWPrime = Double.NaN  // nuevo: guarda el último W′ usado
    var avgBelowCp = 0.0
    const val MAX_DT_SECONDS = 5.0
}

/**
 * Calcula el W' Balance usando modelo Skiba:
 * - Depleción: si P > CP -> W' -= (P - CP) * dt
 * - Recuperación: si P < CP -> tau = W'max / (CP - P)  (Skiba). W'(t+dt) = Wmax - (Wmax - W') * exp(-dt / tau)
 *
 * Se aplican salvaguardas: límites en dt, suavizado de potencia, caps en tau y fallback cuando (CP - P) es muy pequeño.
 */
fun calculateWPrimeBalance(currentPowerInput: Double, userProfile: UserProfile, wPrimeSettings: WPrimeBalanceSettings): Double {
    val now = System.currentTimeMillis()

    // asegurar valores sensatos
    val powerInput = if (currentPowerInput.isFinite() && currentPowerInput >= 0.0) currentPowerInput else 0.0

    // obtener CP y W' max (con fallback seguro) - usar los valores por defecto centralizados en Configdata.kt
    val cpRaw = getCriticalPower(userProfile, wPrimeSettings)
    val cp = if (cpRaw.isFinite() && cpRaw > 0.0) cpRaw else DEFAULT_CP
    val wPrimeMaxRaw = getWPrime(wPrimeSettings)
    val wPrimeMax = if (wPrimeMaxRaw.isFinite() && wPrimeMaxRaw > 0.0) wPrimeMaxRaw else DEFAULT_WPRIME

    synchronized(WPrimeBalanceState) {
        // inicialización
        if (!WPrimeBalanceState.isInitialized) {
            WPrimeBalanceState.currentWPrimeBalance = wPrimeMax
            WPrimeBalanceState.lastUpdateTime = now
            WPrimeBalanceState.smoothedPower = powerInput
            WPrimeBalanceState.lastCp = cp
            WPrimeBalanceState.lastWPrime = wPrimeMax
            WPrimeBalanceState.avgBelowCp = cp      // inicializa la media bajo CP
            WPrimeBalanceState.isInitialized = true
            return 100.0
        }

        // detectar cambio de CP o de W′ mayor al 5 %
        val cpChange = if (!WPrimeBalanceState.lastCp.isNaN())
            kotlin.math.abs(cp - WPrimeBalanceState.lastCp) / WPrimeBalanceState.lastCp
        else 0.0
        val wPrimeChange = if (!WPrimeBalanceState.lastWPrime.isNaN())
            kotlin.math.abs(wPrimeMax - WPrimeBalanceState.lastWPrime) / WPrimeBalanceState.lastWPrime
        else 0.0
        if (cpChange > 0.05 || wPrimeChange > 0.05) {
            WPrimeBalanceState.currentWPrimeBalance = wPrimeMax
            WPrimeBalanceState.smoothedPower = powerInput
            WPrimeBalanceState.lastUpdateTime = now
            WPrimeBalanceState.lastCp = cp
            WPrimeBalanceState.lastWPrime = wPrimeMax
            return 100.0
        }
        WPrimeBalanceState.lastCp = cp
        WPrimeBalanceState.lastWPrime = wPrimeMax



        // calcular dt en segundos, con tope
        var dtSec = (now - WPrimeBalanceState.lastUpdateTime) / 1000.0
        if (dtSec <= 0.0) {
            val pctCurrent = (WPrimeBalanceState.currentWPrimeBalance / wPrimeMax * 1000.0).roundToInt() / 10.0
            return pctCurrent.coerceIn(0.0, 100.0)
        }
        if (dtSec > WPrimeBalanceState.MAX_DT_SECONDS) dtSec = WPrimeBalanceState.MAX_DT_SECONDS

        // suavizado exponencial simple de la potencia (time constant ~1.5s)
        val tauSmooth = 1.5
        val alpha = 1.0 - exp(-dtSec / tauSmooth)
        WPrimeBalanceState.smoothedPower += (powerInput - WPrimeBalanceState.smoothedPower) * alpha
        val power = WPrimeBalanceState.smoothedPower

        // modelo de integración:
        if (power > cp) {
            // depleción directa: energía por encima de CP se resta (W * s = J)
            val energyDepleted = (power - cp) * dtSec
            WPrimeBalanceState.currentWPrimeBalance -= energyDepleted
            if (WPrimeBalanceState.currentWPrimeBalance < 0.0) WPrimeBalanceState.currentWPrimeBalance = 0.0
        } else {
            // Actualiza la media de potencias cuando ruedas por debajo de CP (suavizado ~30 s).
            if (power < cp) {
                val smoothing = dtSec / (dtSec + 30.0)
                WPrimeBalanceState.avgBelowCp += smoothing * (power - WPrimeBalanceState.avgBelowCp)
            }

            // DCP = CP − media de potencias bajo CP. Puede ser 0 si aún no ha bajado de CP.
            val dcp = (cp - WPrimeBalanceState.avgBelowCp).coerceAtLeast(0.0)

            // Cálculo de τ según Skiba usando DCP.
            val tauSkiba = 546.0 * exp(-0.01 * dcp) + 316.0
            val tauUsed = tauSkiba.coerceAtMost(3600.0)

            val expFactor = exp(-dtSec / tauUsed)
            WPrimeBalanceState.currentWPrimeBalance = wPrimeMax - (wPrimeMax - WPrimeBalanceState.currentWPrimeBalance) * expFactor

            // asegurar rangos
            if (WPrimeBalanceState.currentWPrimeBalance > wPrimeMax) WPrimeBalanceState.currentWPrimeBalance = wPrimeMax
            if (WPrimeBalanceState.currentWPrimeBalance < 0.0) WPrimeBalanceState.currentWPrimeBalance = 0.0
        }

        WPrimeBalanceState.lastUpdateTime = now

        // devolver porcentaje con una decimal
        val percentage = (WPrimeBalanceState.currentWPrimeBalance / wPrimeMax * 1000.0).roundToInt() / 10.0
        return percentage.coerceIn(0.0, 100.0)
    }
}

/**
 * Obtiene el Critical Power del perfil del usuario o de la configuración
 */
private fun getCriticalPower(userProfile: UserProfile, wPrimeSettings: WPrimeBalanceSettings): Double {
    return if (wPrimeSettings.useUserFTPAsCP && userProfile.powerZones.size > 3) {

        val ftp = userProfile.ftp.toDouble()
        val cpFromFtp = ftp * 0.96
        if (cpFromFtp.isFinite() && cpFromFtp > 0.0) cpFromFtp else DEFAULT_CP
    } else {
        wPrimeSettings.criticalPower.toDoubleOrNull() ?: DEFAULT_CP
    }
}

/**
 * Obtiene el W' de la configuración del usuario
 */
private fun getWPrime(wPrimeSettings: WPrimeBalanceSettings): Double {
    return wPrimeSettings.wPrime.toDoubleOrNull() ?: DEFAULT_WPRIME
}

