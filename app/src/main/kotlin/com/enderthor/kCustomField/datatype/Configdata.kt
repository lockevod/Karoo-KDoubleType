package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R

enum class KarooAction(val action: String, val label: String, val icon: Int, val color: Int) {
    SPEED(DataType.Type.SPEED, "Speed", R.drawable.ic_speed,R.color.hh_success_green_400),
    HR(DataType.Type.HEART_RATE, "Heart Rate",R.drawable.ic_hr,R.color.hh_success_green_400),
    CADENCE(DataType.Type.CADENCE, "Cadence",R.drawable.ic_cadence,R.color.hh_success_green_400),
    SLOPE(DataType.Type.ELEVATION_GRADE, "Grade",R.drawable.ic_slope,R.color.hh_success_green_400),
    POWER(DataType.Type.POWER, "Power", R.drawable.ic_power,R.color.hh_success_green_400),
    POWER3s(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, "Power 3s", R.drawable.ic_power,R.color.hh_success_green_400),
    DISTANCE(DataType.Type.DISTANCE, "Distance",R.drawable.ic_distance,R.color.hh_success_green_400)
}

@Serializable
data class CustomFieldSettings(
    val customleft1: KarooAction = KarooAction.HR,
    val customright1: KarooAction = KarooAction.SPEED,
    val customleft2: KarooAction = KarooAction.CADENCE,
    val customright2: KarooAction = KarooAction.SLOPE,
){
    companion object {
        val defaultSettings = Json.encodeToString(CustomFieldSettings())
    }
}


