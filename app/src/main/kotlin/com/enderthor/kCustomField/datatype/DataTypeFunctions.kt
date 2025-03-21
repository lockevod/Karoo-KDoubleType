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
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.streamUserProfile
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlin.random.Random
import timber.log.Timber


import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.isActive

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds



class StickyStreamState private constructor() {
    companion object {
        private val lastValidStates = mutableMapOf<String, Pair<Any, Long>>()
        private const val STICKY_TIMEOUT_MS = 7000L // 7 segundos mantiene valores

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
                if (convert == "distance") value / 1609.345 else value * 0.0568182

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
    generalSettings: GeneralSettings
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
                emit(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId = actionId,
                            values = mapOf(DataType.Field.SINGLE to 0.0)
                        )
                    )
                )
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

                    else -> streamDataFlow(action.action)
                        
                        .catch { e ->
                            when (e) {
                                is CancellationException -> {
                                    if (ViewState.isCancelledByEmitter) throw e
                                    Timber.d("Cancelación ignorada en streamDataFlow")
                                    //emit(StreamState.NotAvailable)
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
                        //Timber.d("Emisión streamDataFlow en action.name: ${action.name} con valor $state")

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
        //emit(StreamState.NotAvailable)
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
    isPaletteZwift: Boolean
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

    val iconColor = getColorProvider(context, kaction, iszone)
    val colorZone = if ((kaction.zone in listOf("heartRateZones", "powerZones", "slopeZones")) && iszone) {
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