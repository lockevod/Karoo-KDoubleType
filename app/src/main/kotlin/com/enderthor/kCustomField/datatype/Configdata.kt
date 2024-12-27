package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R


enum class KarooAction(val action: String, val label: String, val icon: Int, val colorday: Int, val colornight: Int,val zone: String, val convert: String) {
    SPEED(DataType.Type.SPEED, "Speed", R.drawable.ic_speed,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","speed"),
    AVERAGE_SPEED(DataType.Type.AVERAGE_SPEED, "Avg Speed", R.drawable.ic_speed_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","speed"),
    HR(DataType.Type.HEART_RATE, "Heart Rate",R.drawable.ic_hr,R.color.hh_success_green_700,R.color.hh_success_green_400,"heartRateZones","none"),
    AVERAGE_HR(DataType.Type.AVERAGE_HR, "Avg HR",R.drawable.ic_hr_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"heartRateZones","none"),
    CADENCE(DataType.Type.CADENCE, "Cadence",R.drawable.ic_cadence,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    AVERAGE_CADENCE(DataType.Type.AVERAGE_CADENCE, "Avg Cadence",R.drawable.ic_cadence_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    SLOPE(DataType.Type.ELEVATION_GRADE, "Grade",R.drawable.ic_slope,R.color.hh_success_green_700,R.color.hh_success_green_400,"slopeZones","none"),
    DISTANCE(DataType.Type.DISTANCE, "Distance",R.drawable.ic_distance,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","distance"),
    POWER(DataType.Type.POWER, "Power", R.drawable.ic_power,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
    POWER3s(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, "Power 3s", R.drawable.ic_power,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
    AVERAGE_POWER(DataType.Type.AVERAGE_POWER, "Avg Power", R.drawable.ic_power_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
   // PEDAL_BALANCE(DataType.Type.PEDAL_POWER_BALANCE, "Pedal Balance",R.drawable.ic_pedal_balance,R.color.hh_success_green_400,"none"),
    //AVERAGE_PEDAL_BALANCE(DataType.Type.AVERAGE_PEDAL_POWER_BALANCE,"Avg Pedal Balance",R.drawable.ic_pedal_balance,R.color.hh_success_green_400,"none"),
}

@Serializable
data class CustomFieldSettings(
    val customleft1: KarooAction,
    val customright1: KarooAction,
    val customleft2: KarooAction,
    val customright2: KarooAction,
    val customleft3: KarooAction,
    val customright3: KarooAction,
    val customleft1zone: Boolean,
    val customright1zone: Boolean,
    val customleft2zone: Boolean,
    val customright2zone: Boolean,
    val customleft3zone: Boolean,
    val customright3zone: Boolean,
    val isvertical1: Boolean,
    val isvertical2: Boolean,
    val isvertical3: Boolean,
    val customverticalleft1: KarooAction,
    val customverticalright1: KarooAction,
    val customverticalleft2: KarooAction,
    val customverticalright2: KarooAction,
    val customverticalleft3: KarooAction,
    val customverticalright3: KarooAction,
    val customverticalleft1zone: Boolean,
    val customverticalright1zone: Boolean,
    val customverticalleft2zone: Boolean,
    val customverticalright2zone: Boolean,
    val customverticalleft3zone: Boolean,
    val customverticalright3zone: Boolean,
    val ishorizontal1: Boolean,
    val ishorizontal2: Boolean,
    val ishorizontal3: Boolean,
    val iscenteralign: Boolean,
)

val defaultSettings = Json.encodeToString(CustomFieldSettings(KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE,KarooAction.CADENCE, KarooAction.SLOPE, false, false, false, false, false,false,false,false,false,KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE, KarooAction.CADENCE, KarooAction.SLOPE,false, false, false,false, false, false, false,false,false, true))

