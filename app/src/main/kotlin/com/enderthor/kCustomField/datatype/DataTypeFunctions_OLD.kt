/*package com.enderthor.kCustomField.datatype

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.FlowPreview
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import com.enderthor.kCustomField.extensions.streamDataFlow
import kotlinx.coroutines.*
import kotlin.random.Random
import timber.log.Timber

object DataTypeCache {
    private const val COLOR_ZONE_CACHE_SIZE = 100
    private const val CONVERT_VALUE_CACHE_SIZE = 200
    private const val COLOR_PROVIDER_CACHE_SIZE = 50
    const val CACHE_TTL = 60000L // 1 minuto

    data class CacheEntry<T>(
        val value: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid() = System.currentTimeMillis() - timestamp < CACHE_TTL
    }

    internal val colorZoneCache = LruCache<String, CacheEntry<ColorProvider>>(COLOR_ZONE_CACHE_SIZE)
    internal val convertValueCache = LruCache<String, CacheEntry<Double>>(CONVERT_VALUE_CACHE_SIZE)
    internal val colorProviderCache = LruCache<String, CacheEntry<ColorProvider>>(COLOR_PROVIDER_CACHE_SIZE)

    fun clearCaches() {
        colorZoneCache.evictAll()
        convertValueCache.evictAll()
        colorProviderCache.evictAll()
        Timber.d("All caches cleared")
    }

    fun putColorZone(key: String, value: ColorProvider) {
        colorZoneCache.put(key, CacheEntry(value))
    }

    fun getColorZone(key: String): CacheEntry<ColorProvider>? {
        return colorZoneCache.get(key)
    }

    fun putConvertValue(key: String, value: Double) {
        convertValueCache.put(key, CacheEntry(value))
    }

    fun getConvertValue(key: String): CacheEntry<Double>? {
        return convertValueCache.get(key)
    }

    fun putColorProvider(key: String, value: ColorProvider) {
        colorProviderCache.put(key, CacheEntry(value))
    }

    fun getColorProvider(key: String): CacheEntry<ColorProvider>? {
        return colorProviderCache.get(key)
    }

    fun getStats() = "ColorZone: ${colorZoneCache.size()}/${COLOR_ZONE_CACHE_SIZE}, " +
            "ConvertValue: ${convertValueCache.size()}/${CONVERT_VALUE_CACHE_SIZE}, " +
            "ColorProvider: ${colorProviderCache.size()}/${COLOR_PROVIDER_CACHE_SIZE}"
}

object StreamMonitor {
    const val WAIT_TIME = 30000L // 30 segundos entre comprobaciones
    const val MAX_RETRIES = 3
    const val RETRY_DELAY = 1000L // 1 segundo entre reintentos

    data class StreamInfo(
        var lastEmission: Long = System.currentTimeMillis(),
        var lastCheck: Long = System.currentTimeMillis(),
        var retryCount: Int = 0,
        var isActive: Boolean = true
    ) {
        fun shouldCheck() = System.currentTimeMillis() - lastCheck >= WAIT_TIME

        fun resetState() {
            lastEmission = System.currentTimeMillis()
            lastCheck = System.currentTimeMillis()
            retryCount = 0
            isActive = true
        }
    }

    private val activeStreams = mutableMapOf<String, StreamInfo>()

    fun registerStream(key: String) {
        activeStreams[key] = StreamInfo()
    }

    fun unregisterStream(key: String) {
        activeStreams.remove(key)
    }

    fun getStreamInfo(key: String): StreamInfo? = activeStreams[key]

    fun updateStreamState(key: String, active: Boolean) {
        activeStreams[key]?.let { info ->
            info.isActive = active
            if (active) info.resetState()
        }
    }
}

fun getColorZone(
    context: Context,
    zone: String,
    value: Double,
    userProfile: UserProfile,
    isPaletteZwift: Boolean
): ColorProvider {
    val cacheKey = "$zone-$value-$isPaletteZwift-${userProfile.hashCode()}"

    DataTypeCache.getColorZone(cacheKey)?.let { cached ->
        if (cached.isValid()) {
            Timber.v("ColorZone cache hit: $cacheKey")
            return cached.value
        }
    }

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
    ).also { color ->
        DataTypeCache.putColorZone(cacheKey, color)
        Timber.v("ColorZone cached: $cacheKey")
    }
}

fun convertValue(
    streamState: StreamState,
    convert: String,
    unitType: UserProfile.PreferredUnit.UnitType,
    type: String
): Double {
    val cacheKey = "${streamState.hashCode()}-$convert-$unitType-$type"

    DataTypeCache.getConvertValue(cacheKey)?.let { cached ->
        if (cached.isValid()) {
            Timber.v("ConvertValue cache hit: $cacheKey")
            return cached.value
        }
    }

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

    return convertedValue.also { result ->
        DataTypeCache.putConvertValue(cacheKey, result)
        Timber.v("ConvertValue cached: $cacheKey")
    }
}

fun getColorProvider(context: Context, action: KarooAction, colorzone: Boolean): ColorProvider {
    val cacheKey = "${action.hashCode()}-$colorzone"

    DataTypeCache.getColorProvider(cacheKey)?.let { cached ->
        if (cached.isValid()) {
            Timber.v("ColorProvider cache hit: $cacheKey")
            return cached.value
        }
    }

    return (if (colorzone) {
        ColorProvider(Color.Black, Color.Black)
    } else {
        ColorProvider(
            day = Color(ContextCompat.getColor(context, action.colorday)),
            night = Color(ContextCompat.getColor(context, action.colornight))
        )
    }).also { color ->
        DataTypeCache.putColorProvider(cacheKey, color)
        Timber.v("ColorProvider cached: $cacheKey")
    }
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
        .debounce(1000 + period)
        .conflate()
        .catch { e ->
            Timber.e(e, "Error in headwindFlow")
            emit(StreamHeadWindData(0.0, 0.0))
        }
        .collect { emit(it) }
}.flowOn(Dispatchers.Default)



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
    maxAttempts: Int = 6,
    initialDelayMillis: Long = 190,
    maxDelayMillis: Long = 600,
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
                    Timber.e(e, "Error en attempt $attempts")
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

*/