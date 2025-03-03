package com.enderthor.kCustomField.extensions

import io.hammerhead.karooext.models.UserProfile
import com.enderthor.kCustomField.R
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue
import kotlin.math.floor

@Serializable
data class zoneslope(val min: Double, val max: Double)

enum class Zone(val colorResource: Int, val colorZwift: Int) {
    Zone0(R.color.zone0, R.color.zone1switft),
    Zone1(R.color.zone9, R.color.zone1switft),
    Zone2(R.color.zone2, R.color.zone2switft),
    Zone3(R.color.zone3, R.color.zone3switft),
    Zone4(R.color.zone4, R.color.zone4switft),
    Zone5(R.color.zone5, R.color.zone5switft),
    Zone6(R.color.zone6, R.color.zone6switft),
    Zone7(R.color.zone7, R.color.zone7switft),
    Zone8(R.color.zone8, R.color.zone7switft)
    //Zone9(R.color.zone1)
}


val slopeZones = listOf(
    zoneslope(min = 0.0, max = 4.6),
    zoneslope(min = 4.601, max = 7.5),
    zoneslope(min = 7.501, max = 12.5),
    zoneslope(min = 12.501, max = 15.5),
    zoneslope(min = 15.501, max = 19.5),
    zoneslope(min = 19.501, max = 23.5),
    zoneslope(min = 23.501, max = 99.5)
)

val zones = mapOf(
    1 to listOf(Zone.Zone7),
    2 to listOf(Zone.Zone1, Zone.Zone7),
    3 to listOf(Zone.Zone1, Zone.Zone3, Zone.Zone7),
    4 to listOf(Zone.Zone1, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    5 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    6 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7, Zone.Zone8),
    7 to listOf(Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    8 to listOf(Zone.Zone1, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    9 to listOf(Zone.Zone1, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone4, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8)
)

inline fun <reified T> getZone(userZones: List<T>, value: Double): Zone? {
    val zoneList = zones[userZones.size] ?: return null
    val valueabs = floor(value.absoluteValue)

    for (i in userZones.indices) {
        val zone = userZones[i]
        val min = when (zone) {
            is UserProfile.Zone -> zone.min.toDouble()
            is zoneslope -> zone.min
            else -> return null
        }
        val max = when (zone) {
            is UserProfile.Zone -> zone.max.toDouble()
            is zoneslope -> zone.max
            else -> return null
        }

        if (valueabs >= min && valueabs < max) {
            return zoneList.getOrNull(i) ?: Zone.Zone7
        }
    }

    // Si el valor es mayor o igual al máximo de la última zona
    val lastZone = userZones.lastOrNull() ?: return null
    val lastMax = when (lastZone) {
        is UserProfile.Zone -> lastZone.max.toDouble()
        is zoneslope -> 99.0
        else -> return null
    }

    if (valueabs >= lastMax) {
        return zoneList.lastOrNull()
    }

    return null
}