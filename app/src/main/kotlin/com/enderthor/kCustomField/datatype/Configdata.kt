package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R

enum class KarooAction(val action: String, val label: String, val icon: Int) {
    SPEED(DataType.Type.SPEED, "Speed", R.drawable.ic_speed),
    POWER(DataType.Type.POWER, "Power", R.drawable.ic_power),
    POWER3s(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, "Power 3s", R.drawable.ic_power),
    CADENCE(DataType.Type.CADENCE, "Cadence",R.drawable.ic_cadence),
    SLOPE(DataType.Type.ELEVATION_GRADE, "Grade",R.drawable.ic_slope),
    HR(DataType.Type.HEART_RATE, "Heart Rate",R.drawable.ic_hr),
   // ELEVATION(DataType.Type.PRESSURE_ELEVATION_CORRECTION, "Elevation",R.drawable.`ic_cadence`),
    DISTANCE(DataType.Type.DISTANCE, "Distance",R.drawable.ic_distance)
}

@Serializable
data class CustomFieldSettings(
    val customleft1: KarooAction = KarooAction.HR,
    val customright1: KarooAction = KarooAction.SPEED,
    val customleft2: KarooAction = KarooAction.HR,
    val customright2: KarooAction = KarooAction.SPEED,
){
    companion object {
        val defaultSettings = Json.encodeToString(CustomFieldSettings())
    }
}


