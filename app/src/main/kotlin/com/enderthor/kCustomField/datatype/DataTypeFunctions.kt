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


import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds


private const val RETRY_CHECK_STREAMS = 4
private const val WAIT_STREAMS_SHORT = 3000L // 3 seconds
private const val WAIT_STREAMS_NORMAL = 60000L // 1 minute
private const val WAIT_STREAMS_LONG = 240000L // 4 minutes
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
fun KarooSystemService.getFieldFlow(
    field: Any,
    headwindFlow: Flow<StreamHeadWindData>,
    generalSettings: GeneralSettings,
): Flow<Any> = flow {

    val (actionId, action) = when (field) {
        is DoubleFieldType -> field.kaction.action to field.kaction
        is OneFieldType -> field.kaction.action to field.kaction
        else -> throw IllegalArgumentException("Tipo de campo no soportado")
    }

    Timber.d("Stream action.name: ${action.name} y actionId: $actionId")

    val streamFlow =
        when {
            action.name == "HEADWIND" && generalSettings.isheadwindenabled -> {
                headwindFlow
                    .onStart {
                        Timber.d("Emisión inicial headwindFlow en action.name: ${action.name} y actionId: $actionId")
                        emit(StreamHeadWindData(0.0, 0.0))
                    }
                    .catch { e ->
                        Timber.e(e, "Error en headwindFlow")
                        throw e  // Se propaga al retryWhen
                    }
                    .timeout(STREAM_TIMEOUT.milliseconds)
            }
            else -> streamDataFlow(action.action)
                .onStart {
                    Timber.d("Emisión inicial streamDataFlow en action.name: ${action.name} y actionId: $actionId")
                    emit(StreamState.Streaming(DataPoint(
                        dataTypeId = actionId,
                        values = mapOf(DataType.Field.SINGLE to 0.0)
                    )))
                }
                .catch { e ->
                    Timber.e(e, "Error en streamDataFlow")
                    throw e  // Se propaga al retryWhen
                }
                .timeout(STREAM_TIMEOUT.milliseconds)
        }

    streamFlow
        .distinctUntilChanged()
        .retryWhen { cause, attempt ->
           if (attempt > RETRY_CHECK_STREAMS) {
                Timber.e("Máximo de reintentos alcanzado en action.name: ${action.name}")
                val backoffDelay = (1000L * (1 shl attempt.toInt()))
                    .coerceAtMost(WAIT_STREAMS_NORMAL)
                delay(backoffDelay)
                true
            } else {
                Timber.w("Reintentando stream action.name: ${action.name}, intento $attempt")
                delay(WAIT_STREAMS_SHORT)
                true
            }
        }
        .collect { state ->
            when (state) {
                is StreamState.Idle, is StreamState.Searching, is StreamState.NotAvailable -> {
                    Timber.d("Stream inactivo ${action.name}  =>: ${state::class.simpleName}")
                    emit(mapOf(DataType.Field.SINGLE to 0.0))
                    delay(STREAM_TIMEOUT)
                }
                 /*is StreamState.NotAvailable -> {
                    Timber.d("Stream inactivo: ${state::class.simpleName}")
                    emit(mapOf(DataType.Field.SINGLE to 0.0))
                    delay(WAIT_STREAMS_LONG)
                }*/
                else -> emit(state)
            }
        }
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