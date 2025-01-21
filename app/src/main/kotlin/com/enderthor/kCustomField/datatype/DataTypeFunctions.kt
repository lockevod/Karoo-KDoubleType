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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.FlowPreview

import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import com.enderthor.kCustomField.extensions.streamDataFlow

import timber.log.Timber

fun getColorZone(context: Context, zone: String, value: Double, userProfile: UserProfile, isPaletteZwift: Boolean): ColorProvider {
    val zoneData = when (zone) {
        "heartRateZones" -> userProfile.heartRateZones
        "powerZones" -> userProfile.powerZones
        else -> slopeZones
    }

    val colorResource = getZone(zoneData, value)?.let { if (isPaletteZwift) it.colorZwift else it.colorResource } ?: R.color.zone7

    return ColorProvider(
        day = Color(ContextCompat.getColor(context, colorResource)),
        night = Color(ContextCompat.getColor(context, colorResource))
    )
}

fun convertValue(streamState: StreamState, convert: String, unitType: UserProfile.PreferredUnit.UnitType, type: String): Double {

    val value = if (type == "TYPE_ELEVATION_REMAINING_ID")
        (streamState as? StreamState.Streaming)?.dataPoint?.values?.get("FIELD_ELEVATION_REMAINING_ID") ?: 0.0
    else (streamState as? StreamState.Streaming)?.dataPoint?.singleValue ?: 0.0

    return when (convert) {
        "distance", "speed" -> when (unitType) {
            UserProfile.PreferredUnit.UnitType.METRIC -> if (convert == "distance") (value / 1000) else (value * 18 / 5)
            UserProfile.PreferredUnit.UnitType.IMPERIAL -> if (convert == "distance") (value / 1609.345) else (value * 0.0568182)
        }
        else -> value
    }
}


fun getColorProvider(context: Context, action: KarooAction, colorzone: Boolean): ColorProvider {
    return if (colorzone) {
        ColorProvider(Color.Black, Color.Black)
    } else {
            ColorProvider(
                day = Color(ContextCompat.getColor(context, action.colorday)),
                night = Color(ContextCompat.getColor(context, action.colornight))
            )
    }
}

fun getFieldSize(size: Int): FieldSize {
    return fieldSizeRanges.first { size in it.min..it.max }.name
}



@OptIn(FlowPreview::class)
fun createHeadwindFlow(karooSystem: KarooSystemService): Flow<StreamHeadWindData> {
    return karooSystem.streamDataFlow(Headwind.DIFF.type)
        .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
        .combine(karooSystem.streamDataFlow(Headwind.SPEED.type)
            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }) { headwindDiff, headwindSpeed ->
            StreamHeadWindData(headwindDiff, headwindSpeed)
        }
        .onStart { emit(StreamHeadWindData(0.0, 0.0)) }
        .distinctUntilChanged()
        .debounce(50)
        .conflate()
        .catch { e ->
            Timber.e(e, "Error in headwindFlow")
            emit(StreamHeadWindData(0.0, 0.0))
        }
}

fun getFieldFlow(karooSystem: KarooSystemService, field: Any, headwindFlow: Flow<StreamHeadWindData>?, generalSettings: GeneralSettings): Flow<Any> {
    return if (field is DoubleFieldType) {
        if (field.kaction.name == "HEADWIND" && generalSettings.isheadwindenabled)
            headwindFlow ?: karooSystem.streamDataFlow(field.kaction.action)
        else karooSystem.streamDataFlow(field.kaction.action)
    } else if (field is OneFieldType) {
        if (field.kaction.name == "HEADWIND" && generalSettings.isheadwindenabled)
            headwindFlow ?: karooSystem.streamDataFlow(field.kaction.action)
        else karooSystem.streamDataFlow(field.kaction.action)
    } else {
        throw IllegalArgumentException("Unsupported field type")
    }
}


fun updateFieldState(fieldState: StreamState, fieldSettings: Any, context: Context, userProfile: UserProfile, isPaletteZwift: Boolean): Quadruple<Double, ColorProvider, ColorProvider,Boolean> {
    val (kaction, iszone) = when (fieldSettings) {
        is DoubleFieldType -> fieldSettings.kaction to fieldSettings.iszone
        is OneFieldType -> fieldSettings.kaction to fieldSettings.iszone
        else -> throw IllegalArgumentException("Unsupported field type")
    }

    val value = convertValue(fieldState, kaction.convert, userProfile.preferredUnit.distance, kaction.action)
    val iconColor = getColorProvider(context, kaction, iszone)
    val colorZone = getColorZone(context, kaction.zone, value, userProfile, isPaletteZwift).takeIf {
        (kaction.zone == "heartRateZones" || kaction.zone == "powerZones" || kaction.zone == "slopeZones") && iszone
    } ?: ColorProvider(Color.White, Color.Black)
    return Quadruple(value, iconColor, colorZone,iszone)
}

fun getFieldState(
    fieldState: Any,
    field: Any,
    context: Context,
    userProfile: UserProfile,
    isPaletteZwift: Boolean
): Quadruple<Double, ColorProvider, ColorProvider,Boolean> {
    return if (fieldState is StreamState) {
        updateFieldState(fieldState, field, context, userProfile, isPaletteZwift)
    } else {
        Quadruple(0.0, ColorProvider(Color.White, Color.Black), ColorProvider(Color.White, Color.Black),false)
    }
}
