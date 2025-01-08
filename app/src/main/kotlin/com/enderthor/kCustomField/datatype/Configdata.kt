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
    val customleft1: KarooAction = KarooAction.SPEED,
    val customright1: KarooAction = KarooAction.SLOPE,
    val customleft2: KarooAction = KarooAction.CADENCE,
    val customright2: KarooAction = KarooAction.POWER3s,
    val customleft3: KarooAction = KarooAction.POWER,
    val customright3: KarooAction = KarooAction.AVERAGE_HR,
    val customleft1zone: Boolean = false,
    val customright1zone: Boolean = true,
    val customleft2zone: Boolean = false,
    val customright2zone: Boolean = true,
    val customleft3zone: Boolean = false,
    val customright3zone: Boolean = false,
    val isvertical1: Boolean = true,
    val isvertical2: Boolean = true,
    val isvertical3: Boolean = true,
    val customverticalleft1: KarooAction = KarooAction.ELEV_GAIN,
    val customverticalright1: KarooAction = KarooAction.ELEV_REMAIN,
    val customverticalleft2: KarooAction = KarooAction.CADENCE,
    val customverticalright2: KarooAction = KarooAction.SLOPE,
    val customverticalleft3: KarooAction= KarooAction.IF,
    val customverticalright3: KarooAction  = KarooAction.TSS,
    val customverticalleft1zone: Boolean =false,
    val customverticalright1zone: Boolean =false,
    val customverticalleft2zone: Boolean = false,
    val customverticalright2zone: Boolean = false,
    val customverticalleft3zone: Boolean = false,
    val customverticalright3zone: Boolean = false,
    val ishorizontal1: Boolean = true,
    val ishorizontal2: Boolean = true,
    val ishorizontal3: Boolean = true,
)

@Serializable
data class OneFieldType(val kaction: KarooAction, val isactive: Boolean, val iszone: Boolean)

@Serializable
enum class RollingTime ( val time: Long) {
    ZERO(0L), FOUR (4000L), TEN (10000L), TWENTY (20000L);
}

@Serializable
data class OneFieldSettings(
    var index: Int = 0,
    var onefield: OneFieldType = OneFieldType(KarooAction.HR, true, true),
    var secondfield: OneFieldType = OneFieldType(KarooAction.SLOPE, false,true),
    var thirdfield: OneFieldType = OneFieldType(KarooAction.SPEED, false,false),
    var rollingtime: RollingTime = RollingTime.ZERO
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
    val iscenterrolling: FieldPosition = FieldPosition.RIGHT,
    val ispalettezwift: Boolean = false,
    val iscenterkaroo: Boolean = false,
)


val defaultSettings = Json.encodeToString(CustomFieldSettings())
val defaultGeneralSettings = Json.encodeToString(GeneralSettings())
val defaultOneFieldSettings = Json.encodeToString(listOf(OneFieldSettings(index=0),OneFieldSettings(index=1),OneFieldSettings(index=2)))
