package com.enderthor.kCustomField.datatype

import android.graphics.BitmapFactory
import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R


data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

enum class Headwind (val type: String) {
    DIFF(DataType.dataTypeId("karoo-headwind", "headwind")),SPEED(DataType.dataTypeId("karoo-headwind", "headwindSpeed"))
}

data class StreamHeadWindData(val diff: Double, val windSpeed: Double)


enum class KarooAction(val action: String, val label: String, val icon: Int, val colorday: Int, val colornight: Int,val zone: String, val convert: String) {
    SPEED(DataType.Type.SPEED, "Speed", R.drawable.ic_speed,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","speed"),
    AVERAGE_SPEED(DataType.Type.AVERAGE_SPEED, "Avg Speed", R.drawable.ic_speed_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","speed"),
    HR(DataType.Type.HEART_RATE, "Heart Rate",R.drawable.ic_hr,R.color.hh_success_green_700,R.color.hh_success_green_400,"heartRateZones","none"),
    AVERAGE_HR(DataType.Type.AVERAGE_HR, "Avg HR",R.drawable.ic_hr_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"heartRateZones","none"),
    CADENCE(DataType.Type.CADENCE, "Cadence",R.drawable.ic_cadence,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    AVERAGE_CADENCE(DataType.Type.AVERAGE_CADENCE, "Avg Cadence",R.drawable.ic_cadence_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    SLOPE(DataType.Type.ELEVATION_GRADE, "Grade",R.drawable.ic_slope,R.color.hh_success_green_700,R.color.hh_success_green_400,"slopeZones","none"),
    DISTANCE(DataType.Type.DISTANCE, "Distance",R.drawable.ic_distance,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","distance"),
    DISTANCE_REMAIN(DataType.Type.DISTANCE_TO_DESTINATION, "Distance Remain",R.drawable.ic_distance,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","distance"),
    POWER(DataType.Type.POWER, "Power", R.drawable.ic_power,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
    POWER3s(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, "Power 3s", R.drawable.ic_power,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
    AVERAGE_POWER(DataType.Type.AVERAGE_POWER, "Avg Power", R.drawable.ic_power_average,R.color.hh_success_green_700,R.color.hh_success_green_400,"powerZones","none"),
    ELEV_GAIN(DataType.Type.ELEVATION_GAIN, "Ascent", R.drawable.ic_elevation,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    ELEV_REMAIN(DataType.Type.ELEVATION_REMAINING, "Ascent Remain", R.drawable.ic_elevation_remain,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    VAM(DataType.Type.VERTICAL_SPEED, "VAM", R.drawable.ic_vam,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    IF(DataType.Type.INTENSITY_FACTOR, "IF", R.drawable.ic_if,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    TSS(DataType.Type.TRAINING_STRESS_SCORE, "TSS", R.drawable.ic_tss,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    HEADWIND(DataType.dataTypeId("karoo-headwind", "headwind"), "Headwind", R.drawable.ic_tss,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
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
)

enum class FieldSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE;
}

data class FieldSizeRange(val name: FieldSize, val min: Int, val max: Int)


enum class FieldPosition {
    LEFT, CENTER, RIGHT;
}

@Serializable
data class RollingTime(val id : String, val name: String, var time: Long)

@Serializable
data class OneFieldType(val kaction: KarooAction, val iszone: Boolean, val isactive: Boolean )

@Serializable
data class OneFieldSettings(
    var index: Int = 0,
    var onefield: OneFieldType = OneFieldType(KarooAction.HR, true, true),
    var secondfield: OneFieldType = OneFieldType(KarooAction.HEADWIND, false,true),
    var thirdfield: OneFieldType = OneFieldType(KarooAction.SPEED, false,true),
    var rollingtime: RollingTime = RollingTime("LOW", "5s", 5000L),
)

@Serializable
data class DoubleFieldType(val kaction: KarooAction,  val iszone: Boolean)

@Serializable
data class DoubleFieldSettings(
    var index: Int = 0,
    var onefield: DoubleFieldType = DoubleFieldType(KarooAction.SPEED, false),
    var secondfield: DoubleFieldType = DoubleFieldType(KarooAction.SLOPE, true),
    val ishorizontal: Boolean = true,
    val isenabled: Boolean = true

)

enum class RefreshTime( val time: Long) {
    ZERO(0L), HALF (500L), ONE (1000L), MID(1500L), TWO (2000L);
}

@Serializable
data class GeneralSettings(
    val iscenteralign: FieldPosition = FieldPosition.RIGHT,
    val iscentervertical: FieldPosition = FieldPosition.CENTER,
    val ispalettezwift: Boolean = false,
    val iscenterkaroo: Boolean = false,
    val isheadwindenabled: Boolean = true,
    val refreshCustom: RefreshTime = RefreshTime.HALF,
    val refreshRolling: RefreshTime = RefreshTime.HALF,
)


val defaultSettings = Json.encodeToString(CustomFieldSettings())
val defaultGeneralSettings = Json.encodeToString(GeneralSettings())
val defaultDoubleFieldSettings = Json.encodeToString(listOf(DoubleFieldSettings(index=0),DoubleFieldSettings(1, DoubleFieldType(KarooAction.CADENCE, false),DoubleFieldType(KarooAction.POWER3s, true),true,true),DoubleFieldSettings(2, DoubleFieldType(KarooAction.IF, false),DoubleFieldType(KarooAction.TSS, false),false,true),DoubleFieldSettings(3, DoubleFieldType(KarooAction.ELEV_GAIN, false),DoubleFieldType(KarooAction.ELEV_REMAIN, false),false,true),DoubleFieldSettings(4, DoubleFieldType(KarooAction.CADENCE, false),DoubleFieldType(KarooAction.SLOPE, true),false,true)))
val defaultOneFieldSettings = Json.encodeToString(listOf(OneFieldSettings(index=0),OneFieldSettings(index=1, OneFieldType(KarooAction.SPEED, false, true),OneFieldType(KarooAction.SPEED, false, false),OneFieldType(KarooAction.POWER, false, false),RollingTime("ZERO", "0s", 0L))))
val defaultRollingTimes = listOf(
    RollingTime("LOW", "5s", 5000L),
    RollingTime("LOW2","10s", 10000L),
    RollingTime("MED", "20s", 20000L),
    RollingTime("UPPER", "30s",30000L)
)
val fieldSizeRanges = listOf(
    FieldSizeRange(FieldSize.SMALL, Int.MIN_VALUE, 13),
    FieldSizeRange(FieldSize.MEDIUM, 14, 15),
    FieldSizeRange(FieldSize.LARGE, 16, 18),
    FieldSizeRange(FieldSize.EXTRA_LARGE, 19, Int.MAX_VALUE)
)

