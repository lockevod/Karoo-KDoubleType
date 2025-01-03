package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R


@Serializable
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
    ELEV_GAIN(DataType.Type.ELEVATION_GAIN, "Ascent", R.drawable.ic_elevation,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    ELEV_REMAIN(DataType.Type.ELEVATION_REMAINING, "Ascent Remain", R.drawable.ic_elevation_remain,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    VAM(DataType.Type.VERTICAL_SPEED, "VAM", R.drawable.ic_vam,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    IF(DataType.Type.INTENSITY_FACTOR, "IF", R.drawable.ic_if,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    TSS(DataType.Type.TRAINING_STRESS_SCORE, "TSS", R.drawable.ic_tss,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
 }

@Serializable
data class CustomFieldSettings(
    val customleft1: KarooAction = KarooAction.HR,
    val customright1: KarooAction = KarooAction.SPEED,
    val customleft2: KarooAction = KarooAction.CADENCE,
    val customright2: KarooAction = KarooAction.SLOPE,
    val customleft3: KarooAction = KarooAction.POWER,
    val customright3: KarooAction = KarooAction.AVERAGE_HR,
    val customleft1zone: Boolean = false,
    val customright1zone: Boolean = false,
    val customleft2zone: Boolean = false,
    val customright2zone: Boolean = false,
    val customleft3zone: Boolean = false,
    val customright3zone: Boolean = false,
    val isvertical1: Boolean = false,
    val isvertical2: Boolean = false,
    val isvertical3: Boolean = false,
    val customverticalleft1: KarooAction = KarooAction.HR,
    val customverticalright1: KarooAction = KarooAction.SPEED,
    val customverticalleft2: KarooAction = KarooAction.CADENCE,
    val customverticalright2: KarooAction = KarooAction.SLOPE,
    val customverticalleft3: KarooAction= KarooAction.POWER,
    val customverticalright3: KarooAction  = KarooAction.AVERAGE_HR,
    val customverticalleft1zone: Boolean =false,
    val customverticalright1zone: Boolean =false,
    val customverticalleft2zone: Boolean = false,
    val customverticalright2zone: Boolean = false,
    val customverticalleft3zone: Boolean = false,
    val customverticalright3zone: Boolean = false,
    val ishorizontal1: Boolean = false,
    val ishorizontal2: Boolean =false,
    val ishorizontal3: Boolean = false,
)


data class FieldSizeRange(val name: FieldSize, val min: Int, val max: Int)

val fieldSizeRanges = listOf(
    FieldSizeRange(FieldSize.SMALL, Int.MIN_VALUE, 13),
    FieldSizeRange(FieldSize.MEDIUM, 14, 15),
    FieldSizeRange(FieldSize.LARGE, 16, 18),
    FieldSizeRange(FieldSize.EXTRA_LARGE, 19, Int.MAX_VALUE)
)

@Serializable
enum class FieldSize {
   SMALL, MEDIUM, LARGE, EXTRA_LARGE;
}

@Serializable
enum class FieldPosition {
    LEFT, CENTER, RIGHT;
}

@Serializable
data class GeneralSettings(
    val iscenteralign: FieldPosition = FieldPosition.RIGHT,
    val iscentervertical: FieldPosition = FieldPosition.CENTER,
    val ispalettezwift: Boolean = false,
    val iscenterkaroo: Boolean = false,
)

val defaultSettings = Json.encodeToString(CustomFieldSettings(KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE,KarooAction.POWER, KarooAction.AVERAGE_HR, false, false, false, false, false,false,false,false,false,KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE, KarooAction.POWER, KarooAction.AVERAGE_HR,false, false, false,false, false, false, false,false,false))
val defaultGeneralSettings = Json.encodeToString(GeneralSettings(FieldPosition.RIGHT,FieldPosition.CENTER,false,false))
