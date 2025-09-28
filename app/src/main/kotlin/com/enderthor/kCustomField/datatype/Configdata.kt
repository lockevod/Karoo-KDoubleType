package com.enderthor.kCustomField.datatype


import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R
import io.hammerhead.karooext.models.DataType.Field
import io.hammerhead.karooext.models.KarooEffect

import io.hammerhead.karooext.models.PlayBeepPattern

import io.hammerhead.karooext.models.UserProfile



const val RETRY_CHECK_STREAMS = 4
const val WAIT_STREAMS_SHORT = 3000L // 3 seconds
const val WAIT_STREAMS_NORMAL = 60000L // 1 minute
const val STREAM_TIMEOUT = 15000L // 15 seconds
const val WAIT_STREAMS_LONG = 120000L // 120 seconds
const val WAIT_STREAMS_MEDIUM = 10000L // 10 seconds
const val DEFAULT_CP = 248.0         // W, fuente única de CP por defecto
const val DEFAULT_WPRIME = 16800.0   // J, fuente única de W' por defecto



data class Quintuple<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

data class ClimbResultData(
    val firstFieldState: Any,
    val secondFieldState: Any,
    val thirdFieldState: Any,
    val fourthFieldState: Any,
    val climbStartFieldState: Any,
    val climbOnFieldState: Any,
    val globalConfig: ClimbGlobalConfigState
)

enum class Headwind (val type: String) {
    DIFF(DataType.dataTypeId("karoo-headwind", "headwind")),SPEED(DataType.dataTypeId("karoo-headwind", "headwindSpeed"))
}

data class StreamHeadWindData(val diff: Double, val windSpeed: Double)


fun getMultiFieldsByAction(karooAction: KarooAction): Triple<String, String,Boolean>? {
    return MultiFields.entries.find { it.action == karooAction.action }?.let {
        Triple(it.left, it.right,it.onlyfirst)
    }
}

enum class MultiFields(val action: String, val left: String, val right: String, val onlyfirst: Boolean)
{
    PEDAL(DataType.Type.PEDAL_SMOOTHNESS, Field.PEDAL_SMOOTHNESS_LEFT, Field.PEDAL_SMOOTHNESS_RIGHT, false),
    TORQUE(DataType.Type.TORQUE_EFFECTIVENESS, Field.TORQUE_EFFECTIVENESS_LEFT, Field.TORQUE_EFFECTIVENESS_RIGHT, false),
    PEDAL_BALANCE(DataType.Type.PEDAL_POWER_BALANCE, Field.PEDAL_POWER_BALANCE_LEFT, Field.PEDAL_POWER_BALANCE_LEFT, true),
    AVERAGE_PEDAL_BALANCE(DataType.Type.AVERAGE_PEDAL_POWER_BALANCE, Field.PEDAL_POWER_BALANCE_LEFT, Field.PEDAL_POWER_BALANCE_LEFT, true),
}

enum class KarooAction(val action: String, val label: String, val icon: Int, val colorday: Int, val colornight: Int, val zone: String, val convert: String, val powerField: Boolean = false) {
    AVERAGE_CADENCE(DataType.Type.AVERAGE_CADENCE, "Avg Cadence", R.drawable.ic_cadence_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    AVERAGE_HR(DataType.Type.AVERAGE_HR, "Avg HR", R.drawable.ic_hr_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "heartRateZones", "none"),
    AVERAGE_PEDAL_BALANCE(DataType.Type.AVERAGE_PEDAL_POWER_BALANCE, "Avg Pedal Balance", R.drawable.ic_pedal_balance_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "pedal", "none", true),
    AVERAGE_POWER(DataType.Type.AVERAGE_POWER, "Avg Power", R.drawable.ic_power_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    AVERAGE_SPEED(DataType.Type.AVERAGE_SPEED, "Avg Speed", R.drawable.ic_speed_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "speed"),
    AVERAGE_VAM(DataType.Type.AVERAGE_VERTICAL_SPEED_30S, "Avg 30s VAM", R.drawable.ic_vam_average, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    ELEV_GAIN(DataType.Type.ELEVATION_GAIN, "Ascent", R.drawable.ic_ascent, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "elevation"),
    ELEV_REMAIN(DataType.Type.ELEVATION_REMAINING, "Ascent Remain", R.drawable.ic_elevation, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "elevation"),
    CADENCE(DataType.Type.CADENCE, "Cadence",R.drawable.ic_cadence,R.color.hh_success_green_700,R.color.hh_success_green_400,"none","none"),
    CIVIL_DAWN(DataType.Type.CIVIL_DAWN, "Civil Dawn", R.drawable.ic_sunrise, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    CIVIL_DUSK(DataType.Type.CIVIL_DUSK, "Civil Dusk", R.drawable.ic_sunset, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    ELEV_LOSS(DataType.Type.ELEVATION_LOSS, "Descent", R.drawable.ic_descent, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "elevation"),
    DISTANCE(DataType.Type.DISTANCE, "Distance", R.drawable.ic_distance, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "distance"),
    DISTANCE_REMAIN(DataType.Type.DISTANCE_TO_DESTINATION, "Distance Remain", R.drawable.ic_distance_remain, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "distance"),
    DISTANCE_FROM_BOTTOM(DataType.Type.DISTANCE_FROM_BOTTOM, "Distance from Bottom", R.drawable.ic_distance_from_bottom, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    DISTANCE_TO_TOP(DataType.Type.DISTANCE_TO_TOP, "Distance to Top", R.drawable.ic_distance_to_top, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    ELEVATION_FROM_BOTTOM(DataType.Type.ELEVATION_FROM_BOTTOM, "Elevation to Bottom", R.drawable.ic_elevation_from_bottom, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "elevation"),
    ELEVATION_TO_TOP(DataType.Type.ELEVATION_TO_TOP, "Elevation to Top", R.drawable.ic_elevation_to_top, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "elevation"),
    //FTP(DataType.dataTypeId("FTP", "FTP"), "FTP", R.drawable.ic_ftp, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    FTPG(DataType.dataTypeId("FTPG", "FTPG"), "FTP", R.drawable.ic_ftp, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    GEARS_FRONT(DataType.Type.SHIFTING_FRONT_GEAR, "Gears Front", R.drawable.ic_front_gear, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    GEARS_REAR(DataType.Type.SHIFTING_REAR_GEAR, "Gears Rear", R.drawable.ic_rear_gear, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    SLOPE(DataType.Type.ELEVATION_GRADE, "Grade", R.drawable.ic_slope, R.color.hh_success_green_700, R.color.hh_success_green_400, "slopeZones", "none"),
    HEADWIND(DataType.dataTypeId("karoo-headwind", "headwind"), "Headwind", R.drawable.ic_no, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    HR(DataType.Type.HEART_RATE, "Heart Rate", R.drawable.ic_hr, R.color.hh_success_green_700, R.color.hh_success_green_400, "heartRateZones", "none"),
    HRPERCENT(DataType.Type.PERCENT_MAX_HR, "HR %", R.drawable.ic_hr_per, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    HR_ZONE(DataType.Type.HR_ZONE, "HR Zone", R.drawable.ic_hr_zone, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    IF(DataType.Type.INTENSITY_FACTOR, "IF", R.drawable.ic_if, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    PEDAL(DataType.Type.PEDAL_SMOOTHNESS, "Pedal Smooth", R.drawable.ic_pedal, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none", true),
    PEDAL_BALANCE(DataType.Type.PEDAL_POWER_BALANCE, "Pedal Balance", R.drawable.ic_pedal_balance, R.color.hh_success_green_700, R.color.hh_success_green_400, "pedal", "none", true),
    POWER(DataType.Type.POWER, "Power", R.drawable.ic_power, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER3s(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, "Power 3s", R.drawable.ic_power_3, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER5s(DataType.Type.SMOOTHED_5S_AVERAGE_POWER, "Power 5s", R.drawable.ic_power_3, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER30s(DataType.Type.SMOOTHED_30S_AVERAGE_POWER, "Power 30s", R.drawable.ic_power_3, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER20m(DataType.Type.SMOOTHED_20M_AVERAGE_POWER, "Power 20m", R.drawable.ic_power_20, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER_NORMALIZED(DataType.Type.NORMALIZED_POWER, "Power Normalized", R.drawable.ic_power_norm, R.color.hh_success_green_700, R.color.hh_success_green_400, "powerZones", "none"),
    POWER_ZONE(DataType.Type.POWER_ZONE, "Power Zone", R.drawable.ic_power_zone, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    SPEED(DataType.Type.SPEED, "Speed", R.drawable.ic_speed, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "speed"),
    TEMPERATURE(DataType.Type.TEMPERATURE, "Temperature", R.drawable.ic_temperature, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    TIMETODEST(DataType.Type.TIME_TO_DESTINATION, "Time to Dest.", R.drawable.ic_time_to_dest, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    TIRE_PRESSURE_FRONT(DataType.Type.TIRE_PRESSURE_FRONT, "Tire Pressure Front", R.drawable.ic_tire_pressure_front, R.color.hh_success_green_700, R.color.hh_success_green_400, "pressure", "none"),
    TIRE_PRESSURE_REAR(DataType.Type.TIRE_PRESSURE_REAR, "Tire Pressure Rear", R.drawable.ic_tire_pressure_rear, R.color.hh_success_green_700, R.color.hh_success_green_400, "pressure", "none"),
    TORQUE(DataType.Type.TORQUE_EFFECTIVENESS, "Torque", R.drawable.ic_torque, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none", true),
    TSS(DataType.Type.TRAINING_STRESS_SCORE, "TSS", R.drawable.ic_tss, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    VAM(DataType.Type.VERTICAL_SPEED, "VAM3s", R.drawable.ic_vam, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    VO2MAX(DataType.dataTypeId("vo2", "VO2max"), "VO2max", R.drawable.ic_vo2max, R.color.hh_success_green_700, R.color.hh_success_green_400, "none", "none"),
    WPRIME_BALANCE(DataType.dataTypeId("WPRIME_BALANCE", "WPRIME_BALANCE"), "W Bal %", R.drawable.ic_battery_charging_60, R.color.hh_success_green_700, R.color.hh_success_green_400, "wprimeZones", "none"),
}

@Serializable
enum class BellBeepPattern(val displayName: String, val tones: List<PlayBeepPattern.Tone>) {

    BELL4("Medium", listOf(
        PlayBeepPattern.Tone(3_800, 900),
        PlayBeepPattern.Tone(0, 300),
        PlayBeepPattern.Tone(3_800, 1000),
    )),
    BELL5(
        "High", listOf(
            PlayBeepPattern.Tone(3_550, 900),
            PlayBeepPattern.Tone(0, 300),
            PlayBeepPattern.Tone(3_550, 1000),
        )),
}


@Serializable
enum class KarooKey(val action: KarooEffect, val label: String) {
    BELL4(PlayBeepPattern(BellBeepPattern.BELL4.tones), "Medium"),
    BELL5(PlayBeepPattern(BellBeepPattern.BELL5.tones), "High"),
}


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
    var secondfield: OneFieldType = OneFieldType(KarooAction.CADENCE,
        iszone = false,
        isactive = false
    ),
    var thirdfield: OneFieldType = OneFieldType(KarooAction.POWER, false, isactive = false),
    var rollingtime: RollingTime = RollingTime("ZERO","0",0L),
    var isextratime: Boolean = false,
)


@Serializable
data class SmartFieldSettings(
    var index: Int = 0,
    var onefield: OneFieldType = OneFieldType(KarooAction.HR, true, true),
    var secondfield: OneFieldType = OneFieldType(KarooAction.CADENCE, false,false),
    var thirdfield: OneFieldType = OneFieldType(KarooAction.POWER, false,false),
    var fourthfield: OneFieldType = OneFieldType(KarooAction.POWER, false,false),
    var rollingfield: OneFieldSettings = OneFieldSettings(),
    var indexRolling: Int = 0,
)

@Serializable
data class DoubleFieldType(val kaction: KarooAction,  val iszone: Boolean)

@Serializable
data class DoubleFieldSettings(
    var index: Int = 0,
    var onefield: DoubleFieldType = DoubleFieldType(KarooAction.SPEED, false),
    var secondfield: DoubleFieldType = DoubleFieldType(KarooAction.SLOPE, true),
    val ishorizontal: Boolean = true,
    val isenabled: Boolean = true,
    )

@Serializable
data class ClimbFieldSettings(
    var index: Int = 0,
    var onefield: DoubleFieldType = DoubleFieldType(KarooAction.SPEED, false),
    var secondfield: DoubleFieldType = DoubleFieldType(KarooAction.SLOPE, true),
    var thirdfield: DoubleFieldType = DoubleFieldType(KarooAction.CADENCE, false),
    var fourthfield: DoubleFieldType = DoubleFieldType(KarooAction.POWER3s, true),
    var climbfield: DoubleFieldType = DoubleFieldType(KarooAction.HR, true),
    var climbOnfield: DoubleFieldType = DoubleFieldType(KarooAction.DISTANCE_TO_TOP, false),
    val isenabled: Boolean = true,
    val isAlwaysClimbPos: Boolean = false,
    val isfirsthorizontal: Boolean = true,
    val issecondhorizontal: Boolean = true,
)

@Serializable
data class powerSettings(
    val powerLoss: String = "2.2",
    val rollingResistanceCoefficient: String = "0.0095",
    val bikeMass: String = "14.0",
    val surface: String = "1.05"
)

@Serializable
data class WPrimeBalanceSettings(
    val criticalPower: String = DEFAULT_CP.toInt().toString(),        // CP por defecto desde constante
    val wPrime: String = DEFAULT_WPRIME.toInt().toString(),           // W' por defecto desde constante
    val useUserFTPAsCP: Boolean = true,
    val useVisualZones: Boolean = true
)

enum class RefreshTime( val time: Long) {
    HALF (400L),  MID(800L) , EXTRA_ROLLING(200L),
}

enum class Delay( val time: Long) {
    PREVIEW (2000L), RETRY_SHORT (2000L), RETRY_LONG (6000L),
}

@Serializable
data class GeneralSettings(
    val iscenteralign: FieldPosition = FieldPosition.RIGHT,
    val iscentervertical: FieldPosition = FieldPosition.CENTER,
    val ispalettezwift: Boolean = false,
    val iscenterkaroo: Boolean = false,
    val isheadwindenabled: Boolean = false,
    val refreshCustom: RefreshTime = RefreshTime.HALF,
    val refreshRolling: RefreshTime = RefreshTime.HALF,
    val isdivider: Boolean = true,
    val bellBeepKey: KarooKey = KarooKey.BELL4,
)

data class GlobalConfigState(
    val settings: List<DoubleFieldSettings>,
    val generalSettings: GeneralSettings,
    val userProfile: UserProfile? = null
)

data class ClimbGlobalConfigState(
    val settings: List<ClimbFieldSettings>,
    val generalSettings: GeneralSettings,
    val userProfile: UserProfile? = null
)


val defaultGeneralSettings = Json.encodeToString(GeneralSettings())
val previewDoubleFieldSettings = listOf(DoubleFieldSettings(index=0),DoubleFieldSettings(1, DoubleFieldType(KarooAction.CADENCE, false),DoubleFieldType(KarooAction.POWER3s, true),true,true),DoubleFieldSettings(2, DoubleFieldType(KarooAction.IF, false),DoubleFieldType(KarooAction.TSS, false),false,true),DoubleFieldSettings(3, DoubleFieldType(KarooAction.ELEV_GAIN, false),DoubleFieldType(KarooAction.ELEV_REMAIN,false),false,true),DoubleFieldSettings(4, DoubleFieldType(KarooAction.PEDAL_BALANCE, false),DoubleFieldType(KarooAction.AVERAGE_PEDAL_BALANCE, true),false,true),DoubleFieldSettings(5, DoubleFieldType(KarooAction.CADENCE, false),DoubleFieldType(KarooAction.WPRIME_BALANCE, true),false,true))
val defaultDoubleFieldSettings = Json.encodeToString(previewDoubleFieldSettings)
val previewOneFieldSettings = listOf(OneFieldSettings(index=0),OneFieldSettings(1, OneFieldType(KarooAction.POWER_NORMALIZED, false,
    isactive = true
),OneFieldType(KarooAction.POWER20m, false, true),OneFieldType(KarooAction.SPEED, false, false),RollingTime("MED", "20s", 20000L)),OneFieldSettings(2, OneFieldType(KarooAction.DISTANCE, false, true),OneFieldType(KarooAction.DISTANCE_REMAIN, false, true),OneFieldType(KarooAction.TIMETODEST, false, true),RollingTime("MED", "20s", 20000L)))
val defaultOneFieldSettings = Json.encodeToString(previewOneFieldSettings)
val defaultPowerSettings = Json.encodeToString(powerSettings())
val defaultWPrimeBalanceSettings = Json.encodeToString(WPrimeBalanceSettings())
val previewSmartFieldSettings = listOf(SmartFieldSettings(index=0))
val defaultSmartFieldSettings = Json.encodeToString(previewSmartFieldSettings)
val previewClimbFieldSettings = listOf(ClimbFieldSettings(index=0))
val defaultClimbFieldSettings = Json.encodeToString(previewClimbFieldSettings)

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