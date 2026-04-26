@file:Suppress("unused")

package com.enderthor.kCustomField.manager

import android.content.Context
import android.net.Uri
import com.enderthor.kCustomField.datatype.*
import com.enderthor.kCustomField.extensions.*
import com.enderthor.kCustomField.io.readStringFromUri
import com.enderthor.kCustomField.io.writeStringToUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlinx.serialization.json.Json

/**
 * Manager responsable de exportar/importar configuraciones, crear backups y aplicar payloads.
 * Usa las funciones de guardado existentes (save* en extensions) para aplicar las secciones.
 */
object ImportExportManager {

    // Public action name for config-applied broadcasts
    const val ACTION_CONFIG_APPLIED = "com.enderthor.kCustomField.ACTION_CONFIG_APPLIED"

    // Ensure defaults are encoded so exported JSON contains full objects instead of {} when all
    // values are defaults. This makes imports clearer and more robust across versions.
    private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun buildExportConfig(ctx: Context): ExportConfig = withContext(Dispatchers.IO) {
        val general = ctx.streamGeneralSettings().first()
        val doubleFields = ctx.streamDoubleFieldSettings().first()
        val sextupleFields = ctx.streamSextupleFieldSettings().first()
        val oneFields = ctx.streamOneFieldSettings().first()
        val smartFields = ctx.streamSmartFieldSettings().first()
        val climbFields = ctx.streamClimbFieldSettings().first()
        val power = ctx.streamStoredPowerSettings().first()
        val wbal = ctx.streamWPrimeBalanceSettings().first()
        // read stored rolling times so exported payload contains the actual user-configured options
        val rolling = try {
            ctx.streamRollingTimes().first()
        } catch (e: Exception) {
            Timber.w(e, "Failed to read stored rollingTimes, falling back to defaults")
            defaultRollingTimes
        }

        val payload = ExportPayload(
             generalSettings = general,
             doubleFieldSettings = doubleFields,
             sextupleFieldSettings = sextupleFields,
             oneFieldSettings = oneFields,
             smartFieldSettings = smartFields,
             climbFieldSettings = climbFields,
             powerSettings = power,
             wprimeBalanceSettings = wbal,
             rollingTimes = rolling
         )

        // Log the payload serialized with defaults included so we can verify what's exported (Conf fields etc.)
        try {
            val payloadText = prettyJson.encodeToString(ExportPayload.serializer(), payload)
            Timber.d("ExportPayload JSON: %s", payloadText)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize export payload for logging")
        }

        // Try to include appVersion (versionName) for traceability
        val appVer = try {
            val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pkg?.versionName
        } catch (e: Exception) {
            Timber.w(e, "Failed to read package version")
            null
        }

        ExportConfig(
            schemaVersion = CURRENT_EXPORT_SCHEMA_VERSION,
            appVersion = appVer,
            createdAt = System.currentTimeMillis().toString(),
            payload = payload
        )
    }

    suspend fun exportToUri(ctx: Context, uri: Uri) {
        val cfg = buildExportConfig(ctx)
        val text = prettyJson.encodeToString(ExportConfig.serializer(), cfg)
        writeStringToUri(ctx, uri, text)
    }

    // Public helper to serialize an ExportConfig using the internal prettyJson (encodeDefaults=true)
    fun exportConfigToText(cfg: ExportConfig): String {
        return prettyJson.encodeToString(ExportConfig.serializer(), cfg)
    }

    suspend fun readExportFromUri(ctx: Context, uri: Uri): ExportConfig = withContext(Dispatchers.IO) {
        val text = readStringFromUri(ctx, uri)
        jsonWithUnknownKeys.decodeFromString<ExportConfig>(text)
    }

    suspend fun parseExportFromText(text: String): ExportConfig = withContext(Dispatchers.Default) {
        jsonWithUnknownKeys.decodeFromString<ExportConfig>(text)
    }

    suspend fun writeBackupToUri(ctx: Context, uri: Uri, text: String) {
        writeStringToUri(ctx, uri, text)
    }

    // In-process event flow to notify UI components that a config has been applied
    val configApplied = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    suspend fun applyPayload(ctx: Context, payload: ExportPayload) {
        // apply known sections using existing save helpers
        payload.generalSettings?.let { saveGeneralSettings(ctx, it) }
        payload.doubleFieldSettings?.let { saveDoubleFieldSettings(ctx, it) }
        payload.sextupleFieldSettings?.let { saveSextupleFieldSettings(ctx, it) }
        payload.oneFieldSettings?.let { saveOneFieldSettings(ctx, it) }
        payload.smartFieldSettings?.let { saveSmartFieldSettings(ctx, it) }
        payload.climbFieldSettings?.let { saveClimbFieldSettings(ctx, it) }
        payload.powerSettings?.let { savePowerSettings(ctx, it) }
        payload.wprimeBalanceSettings?.let { saveWPrimeBalanceSettings(ctx, it) }
        // persist rollingTimes directly into DataStore to avoid compile-order issues
        payload.rollingTimes?.let { times ->
            try {
                saveRollingTimes(ctx, times)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist rollingTimes during import")
            }
        }
        // Notify in-process listeners that config was applied so screens can refresh immediately
        try {
            configApplied.tryEmit(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Failed to emit config applied event")
        }
    }

    fun sectionsPresent(payload: ExportPayload?): String {
        if (payload == null) return ""
        val list = mutableListOf<String>()
        if (payload.generalSettings != null) list.add("General")
        if (payload.doubleFieldSettings != null) list.add("Custom Fields")
        if (payload.sextupleFieldSettings != null) list.add("Sextuple Fields")
        if (payload.oneFieldSettings != null) list.add("Rolling Fields")
        if (payload.smartFieldSettings != null) list.add("Smart Fields")
        if (payload.climbFieldSettings != null) list.add("Climb Fields")
        if (payload.powerSettings != null) list.add("Power Settings")
        if (payload.wprimeBalanceSettings != null) list.add("W' Balance")
        if (payload.rollingTimes != null) list.add("Rolling Times")
        return list.joinToString(", ")
    }
}
