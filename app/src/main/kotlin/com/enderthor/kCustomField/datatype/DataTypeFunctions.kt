// DataTypeFunctions.kt
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.FlowPreview
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import com.enderthor.kCustomField.extensions.streamDataFlow
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlin.random.Random
import timber.log.Timber
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException


private const val RETRY_CHECK_STREAMS = 3
private const val WAIT_STREAMS = 120000L // 120 seconds
private const val STREAM_TIMEOUT = 15000L // 15 seconds

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
        else -> (streamState as? StreamState.Streaming)?.dataPoint?.singleValue
    } ?: 0.0

    val convertedValue = when (convert) {
        "distance", "speed" -> when (unitType) {
            UserProfile.PreferredUnit.UnitType.METRIC ->
                if (convert == "distance") value / 1000 else value * 3.6
            UserProfile.PreferredUnit.UnitType.IMPERIAL ->
                if (convert == "distance") value / 1609.345 else value * 0.0568182
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
}.flowOn(Dispatchers.Default)

@OptIn(FlowPreview::class)
private suspend fun <T> Flow<T>.monitorStream(
    fieldName: String,
    lastEmissionTime: AtomicLong
): Flow<T> = transform { value ->
    val currentTime = System.currentTimeMillis()
    lastEmissionTime.set(currentTime)
    emit(value)
}.catch { e ->
    Timber.e(e, "Stream error in $fieldName")
    throw e
}


fun getFieldFlow(
    karooSystem: KarooSystemService,
    field: Any,
    headwindFlow: Flow<StreamHeadWindData>,
    generalSettings: GeneralSettings,
    period: Long
): Flow<Any> = flow {
    val lastEmissionTime = AtomicLong(System.currentTimeMillis())
    var retryCount = 0
    var lastCheckTime = 0L
    var isFirstEmission = true

    while (true) {
        try {
            val streamFlow = when (field) {
                is DoubleFieldType, is OneFieldType -> {
                    val action = when (field) {
                        is DoubleFieldType -> field.kaction
                        is OneFieldType -> field.kaction
                        else -> throw IllegalArgumentException("Invalid field type")
                    }

                    when {
                        action.name == "HEADWIND" && generalSettings.isheadwindenabled ->
                            headwindFlow.monitorStream("Headwind", lastEmissionTime)
                        else -> karooSystem.streamDataFlow(action.action, period)
                            .monitorStream(action.label, lastEmissionTime)
                    }
                }
                else -> throw IllegalArgumentException("Unsupported field type")
            }

            // Asegurar emisión inicial
            if (isFirstEmission) {
                emit(StreamState.Streaming(DataPoint(
                    dataTypeId = when (field) {
                        is DoubleFieldType -> field.kaction.action
                        is OneFieldType -> field.kaction.action
                        else -> ""
                    },
                    values = mapOf(DataType.Field.SINGLE to 0.0)
                )))
                isFirstEmission = false
            }

            streamFlow
                .map { state ->
                    when (state) {
                        is StreamState.Idle, is StreamState.NotAvailable -> {
                            Timber.d("Stream in inactive state (${state::class.simpleName}), waiting...")
                            // Emitir último valor conocido o valor por defecto
                            emit(state)
                            delay(WAIT_STREAMS)
                            state
                        }
                        else -> state
                    }
                }
                .distinctUntilChanged() // Evitar emisiones duplicadas
                .onStart {
                    // Emitir estado inicial
                    emit(StreamState.Streaming(DataPoint(
                        dataTypeId = when (field) {
                            is DoubleFieldType -> field.kaction.action
                            is OneFieldType -> field.kaction.action
                            else -> ""
                        },
                        values = mapOf(DataType.Field.SINGLE to 0.0)
                    )))
                }
                .takeWhile {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastEmission = currentTime - lastEmissionTime.get()

                    if (timeSinceLastEmission > STREAM_TIMEOUT && currentTime - lastCheckTime > WAIT_STREAMS) {
                        lastCheckTime = currentTime
                        if (retryCount < RETRY_CHECK_STREAMS) {
                            retryCount++
                            Timber.w("Stream timeout detected, attempt $retryCount/$RETRY_CHECK_STREAMS")
                            false
                        } else {
                            Timber.e("Max retries reached, waiting before next check")
                            delay(WAIT_STREAMS)
                            retryCount = 0
                            false
                        }
                    } else {
                        true
                    }
                }
                .catch { e ->
                    when (e) {
                        is CancellationException -> {
                            Timber.d("Flow cancelled, restarting stream")
                            delay(1000)
                            throw e
                        }
                        else -> throw e
                    }
                }
                .collect {
                    emit(it)
                }

        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    Timber.d("Stream cancelled, attempting restart after brief delay")
                    delay(1000)
                    continue
                }
                else -> {
                    Timber.e(e, "Error in stream flow")
                    if (retryCount < RETRY_CHECK_STREAMS) {
                        retryCount++
                        delay(1000)
                    } else {
                        retryCount = 0
                        delay(WAIT_STREAMS)
                    }
                }
            }
        }
    }
}.flowOn(Dispatchers.Default)
    .catch { e ->
        Timber.e(e, "Error in outer flow")
        throw e
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
    } else {
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
}.flowOn(Dispatchers.IO)