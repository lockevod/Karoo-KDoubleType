package com.enderthor.kCustomField.extensions

import io.hammerhead.karooext.models.UserProfile
import com.enderthor.kCustomField.R
import kotlinx.serialization.Serializable

@Serializable
data class zoneslope(val min: Double, val max: Double)

enum class Zone(val colorResource: Int){
    Zone0(R.color.zone0),
    Zone1(R.color.zone1),
    Zone2(R.color.zone2),
    Zone3(R.color.zone3),
    Zone4(R.color.zone4),
    Zone5(R.color.zone5),
    Zone6(R.color.zone6),
    Zone7(R.color.zone7),
    Zone8(R.color.zone8),
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
    2 to listOf(Zone.Zone0, Zone.Zone7),
    3 to listOf(Zone.Zone0, Zone.Zone3, Zone.Zone7),
    4 to listOf(Zone.Zone0, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    5 to listOf(Zone.Zone0, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7),
    6 to listOf(Zone.Zone0, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone7, Zone.Zone8),
    7 to listOf(Zone.Zone0, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    8 to listOf(Zone.Zone0, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8),
    9 to listOf(Zone.Zone0, Zone.Zone1, Zone.Zone2, Zone.Zone3, Zone.Zone4, Zone.Zone5, Zone.Zone6, Zone.Zone7, Zone.Zone8)
)

inline fun <reified T> getZone(userZones: List<T>, value: Double): Zone? {
    val zoneList = zones[userZones.size] ?: return null

    userZones.forEachIndexed { index, zone ->
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
        if (value in min..max) {
            return zoneList.getOrNull(index) ?: Zone.Zone7
        }
    }

    return null
}