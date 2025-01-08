package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile

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

fun getColorFilter(context: Context, action: KarooAction, colorzone: Boolean): ColorFilter {
    return if (colorzone) {
        ColorFilter.tint(ColorProvider(Color.Black, Color.Black))
    } else {
        ColorFilter.tint(
            ColorProvider(
                day = Color(ContextCompat.getColor(context, action.colorday)),
                night = Color(ContextCompat.getColor(context, action.colornight))
            )
        )
    }
}

fun getFieldSize(size: Int): FieldSize {
    return fieldSizeRanges.first { size in it.min..it.max }.name
}