package com.enderthor.kCustomField.datatype

import kotlinx.serialization.Serializable

// Version actual del esquema de export/import
const val CURRENT_EXPORT_SCHEMA_VERSION = 1

@Serializable
data class ExportConfig(
    val schemaVersion: Int = CURRENT_EXPORT_SCHEMA_VERSION,
    val appVersion: String? = null,
    val createdAt: String? = null,
    val payload: ExportPayload? = null
)

@Serializable
data class ExportPayload(
    val generalSettings: GeneralSettings? = null,
    val doubleFieldSettings: List<DoubleFieldSettings>? = null,
    val sextupleFieldSettings: List<SextupleFieldSettings>? = null,
    val oneFieldSettings: List<OneFieldSettings>? = null,
    val smartFieldSettings: List<SmartFieldSettings>? = null,
    val climbFieldSettings: List<ClimbFieldSettings>? = null,
    val powerSettings: powerSettings? = null,
    val wprimeBalanceSettings: WPrimeBalanceSettings? = null,
    val rollingTimes: List<RollingTime>? = null
)
