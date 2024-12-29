package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.models.DataType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.enderthor.kCustomField.R


/**
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Quadruple exhibits value semantics
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 */

@Serializable
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)  {

    /**
     * Returns string representation of the [Quadruple] including its [first], [second], [third] and [fourth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * Converts this quadruple into a list.
 */



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
)


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

val defaultSettings = Json.encodeToString(CustomFieldSettings(KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE,KarooAction.CADENCE, KarooAction.SLOPE, false, false, false, false, false,false,false,false,false,KarooAction.HR, KarooAction.SPEED, KarooAction.CADENCE, KarooAction.SLOPE, KarooAction.CADENCE, KarooAction.SLOPE,false, false, false,false, false, false, false,false,false))
val defaultGeneralSettings = Json.encodeToString(GeneralSettings(FieldPosition.RIGHT,FieldPosition.CENTER,false,false))
