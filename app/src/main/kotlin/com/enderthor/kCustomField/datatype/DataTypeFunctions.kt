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
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import timber.log.Timber


import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.isActive

import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds



class StickyStreamState private constructor() {
    companion object {
        private val lastValidStates = mutableMapOf<String, Pair<Any, Long>>()
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

                if (state !is StreamState.Searching || Random.nextInt(20) == 0) {
                    Timber.d("Usando valor almacenado para $actionName, edad: ${currentTime - timestamp}ms")
                }
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

    val value = when (type) {
        "TYPE_ELEVATION_REMAINING_ID" -> (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_REMAINING_ID")
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
        else -> value
    }

    return convertedValue
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

        Timber.d("Stream action.name: ${action.name} y actionId: $actionId")


            if (action.name == "HEADWIND" && generalSettings.isheadwindenabled) {
                Timber.d("Emisión inicial headwindFlow en action.name: ${action.name}")
                emit(StreamHeadWindData(0.0, 0.0))
            } else {
                Timber.d("Emisión inicial streamDataFlow en action.name: ${action.name}")
                // No emitimos nada inicialmente.
                /*emit(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId = actionId,
                            values = mapOf(DataType.Field.SINGLE to 0.0)
                        )
                    )
                )*/
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
                                        Timber.d("Cancelación ignorada en headwindFlow")
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
                        // Combinar los streams de power, user profile y configuraciones de W' Balance
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
                        .timeout(STREAM_TIMEOUT.milliseconds)
                    }

                    else -> streamDataFlow(action.action)
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    if (ViewState.isCancelledByEmitter) throw e
                                    Timber.d("Cancelación ignorada en streamDataFlow")
                                }
                                else -> {
                                    Timber.e(e, "Error en streamDataFlow")
                                    emit(StreamState.NotAvailable)
                                }
                            }
                        }
                        .timeout(STREAM_TIMEOUT.milliseconds)
                }

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
    if (powerValue <= 0) return 0.0

    val weight = userProfile.weight
    if (weight <= 0) return powerValue * 0.95

    // Allen & Coggan
    val ftpStandard = powerValue * 0.95

    //  Coogan & Allen Improve
    val wattsPerKg = powerValue / weight
    val ftpAdjusted = when {
        wattsPerKg > 4.0 -> powerValue * 0.97
        wattsPerKg > 3.0 -> powerValue * 0.95
        wattsPerKg > 2.0 -> powerValue * 0.93
        else -> powerValue * 0.90
    }

    // Historical FTP

    val ftpHistorical = userProfile.powerZones.let { zones ->
        if (zones.isNotEmpty()) {

            val currentFTP = zones[3].max
            if (currentFTP > 0) {
                val changeLimit = 0.05
                val minFTP = currentFTP * (1 - changeLimit)
                val maxFTP = currentFTP * (1 + changeLimit)
                ftpStandard.coerceIn(minFTP, maxFTP)
            } else {
                ftpStandard
            }
        } else {
            ftpStandard
        }
    }


    return ((ftpStandard * 0.4) + (ftpAdjusted * 0.3) + (ftpHistorical * 0.3)).roundToInt().toDouble()
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

    Timber.d("updateFieldState: fieldSettings: $fieldSettings, kaction: ${kaction.name}, iszone: $iszone")
    val (value, valueRight) = if (kaction.powerField) {
        multipleStreamValues(fieldState, kaction)
    } else {
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

fun <T> retryFlow(
    maxAttempts: Int = 8,
    initialDelayMillis: Long = 80,
    maxDelayMillis: Long = 400,
    action: suspend FlowCollector<T>.() -> Unit,
    onFailure: suspend FlowCollector<T>.(Int, Throwable) -> Unit
): Flow<T> = flow {
    var attempts = 0
    var delayMillis = initialDelayMillis

    while (attempts < maxAttempts) {
        try {
            action()
            return@flow
        } catch (e: Throwable) {
            when (e) {
                is IndexOutOfBoundsException, is Exception -> {
                    if (e is CancellationException) {
                        Timber.d("Job cancelled during attempt $attempts")
                    } else {
                        Timber.e(e, "Error en attempt $attempts")
                    }
                    attempts++
                    if (attempts >= maxAttempts) {
                        onFailure(attempts, e)
                    } else {
                        delay(delayMillis + Random.nextLong(0, delayMillis / 2))
                        delayMillis = (delayMillis * 2).coerceAtMost(maxDelayMillis)
                    }
                }
                else -> throw e
            }
        }
    }
}
//ADD

// Estado del modelo diferencial W' Balance Prime - Variables estáticas para mantener estado
private object WPrimeBalanceState {
    var currentWPrimeBalance = 0.0
    var lastUpdateTime = 0L
    var isInitialized = false

    // Constantes del modelo Diferencial de Froncioni/Clarke
    const val TAU_W_PLUS = 546.0  // Constante de tiempo para recuperación (segundos)
    const val TAU_W_MINUS = 316.0 // Constante de tiempo para depleción (segundos)
    const val DEFAULT_CP = 250.0  // Critical Power por defecto (W)
    const val DEFAULT_WPRIME = 20000.0 // W' por defecto (J)
}

/**
 * Calcula el W' Balance usando el modelo diferencial de Froncioni/Clarke
 * Variante optimizada para tiempo real, no requiere histórico completo
 * Devuelve el porcentaje de W' Balance restante como número entero (0-100%)
 */
fun calculateWPrimeBalance(currentPower: Double, userProfile: UserProfile, wPrimeSettings: WPrimeBalanceSettings): Double {
    val currentTime = System.currentTimeMillis()

    // Obtener CP y W' desde la configuración del usuario
    val cp = getCriticalPower(userProfile, wPrimeSettings)
    val wPrime = getWPrime(wPrimeSettings)
    val tauWPlus = wPrimeSettings.tauWPlus.toDoubleOrNull() ?: WPrimeBalanceState.TAU_W_PLUS
    val tauWMinus = wPrimeSettings.tauWMinus.toDoubleOrNull() ?: WPrimeBalanceState.TAU_W_MINUS

    if (!WPrimeBalanceState.isInitialized) {
        // Inicializar con W' completo
        WPrimeBalanceState.currentWPrimeBalance = wPrime
        WPrimeBalanceState.lastUpdateTime = currentTime
        WPrimeBalanceState.isInitialized = true
        return 100.0 // 100% de batería al inicio
    }

    val deltaTime = (currentTime - WPrimeBalanceState.lastUpdateTime) / 1000.0 // Convertir a segundos

    if (deltaTime <= 0) {
        // Devolver como porcentaje entero
        return (WPrimeBalanceState.currentWPrimeBalance / wPrime * 100.0).roundToInt().toDouble().coerceIn(0.0, 100.0)
    }

    // Modelo diferencial de Froncioni/Clarke
    when {
        currentPower > cp -> {
            // Potencia por encima de CP - depleción
            val powerAboveCP = currentPower - cp

            // dW'/dt = -(P - CP) - W'/τ_w-
            val dWPrimeDt = -powerAboveCP - (WPrimeBalanceState.currentWPrimeBalance / tauWMinus)
            WPrimeBalanceState.currentWPrimeBalance += dWPrimeDt * deltaTime

            // W' no puede ser negativo
            WPrimeBalanceState.currentWPrimeBalance = kotlin.math.max(0.0, WPrimeBalanceState.currentWPrimeBalance)
        }
        else -> {
            // Potencia por debajo o igual a CP - recuperación

            // dW'/dt = (W'_max - W') / τ_w+
            val dWPrimeDt = (wPrime - WPrimeBalanceState.currentWPrimeBalance) / tauWPlus
            WPrimeBalanceState.currentWPrimeBalance += dWPrimeDt * deltaTime

            // W' no puede exceder W'_max
            if (WPrimeBalanceState.currentWPrimeBalance > wPrime) {
                WPrimeBalanceState.currentWPrimeBalance = wPrime
            }
        }
    }

    WPrimeBalanceState.lastUpdateTime = currentTime

    // Devolver como porcentaje entero (0-100%)
    return (WPrimeBalanceState.currentWPrimeBalance / wPrime * 100.0).roundToInt().toDouble().coerceIn(0.0, 100.0)
}

/**
 * Obtiene el Critical Power del perfil del usuario o de la configuración
 */
private fun getCriticalPower(userProfile: UserProfile, wPrimeSettings: WPrimeBalanceSettings): Double {
    return if (wPrimeSettings.useUserFTPAsCP && userProfile.powerZones.size > 3) {
        userProfile.powerZones[3].max.toDouble() // FTP como aproximación de CP
    } else {
        wPrimeSettings.criticalPower.toDoubleOrNull() ?: WPrimeBalanceState.DEFAULT_CP
    }
}

/**
 * Obtiene el W' de la configuración del usuario
 */
private fun getWPrime(wPrimeSettings: WPrimeBalanceSettings): Double {
    return wPrimeSettings.wPrime.toDoubleOrNull() ?: WPrimeBalanceState.DEFAULT_WPRIME
}
