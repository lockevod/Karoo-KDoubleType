@file:Suppress("unused")

package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enderthor.kCustomField.datatype.ClimbFieldSettings

import com.enderthor.kCustomField.datatype.DoubleFieldSettings
import com.enderthor.kCustomField.datatype.SextupleFieldSettings
import com.enderthor.kCustomField.datatype.GeneralSettings
import com.enderthor.kCustomField.datatype.OneFieldSettings
import com.enderthor.kCustomField.datatype.SmartFieldSettings
import com.enderthor.kCustomField.datatype.defaultClimbFieldSettings
import com.enderthor.kCustomField.datatype.defaultDoubleFieldSettings
import com.enderthor.kCustomField.datatype.defaultSextupleFieldSettings
import com.enderthor.kCustomField.datatype.defaultGeneralSettings
import com.enderthor.kCustomField.datatype.defaultOneFieldSettings
import com.enderthor.kCustomField.datatype.defaultSmartFieldSettings
import com.enderthor.kCustomField.datatype.defaultPowerSettings
import com.enderthor.kCustomField.datatype.powerSettings
import com.enderthor.kCustomField.datatype.WPrimeBalanceSettings
import com.enderthor.kCustomField.datatype.defaultWPrimeBalanceSettings
import com.enderthor.kCustomField.datatype.RollingTime
import com.enderthor.kCustomField.datatype.defaultRollingTimes

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import timber.log.Timber

// coerceInputValues: si en una versión futura se elimina o renombra un valor de KarooAction
// (u otro enum), el valor desconocido se sustituye por el default de la propiedad en vez de
// lanzar SerializationException — que el catch convertía en "resetear TODA la configuración
// del usuario a los defaults". Requiere que las propiedades enum tengan default (lo tienen:
// DoubleFieldType/OneFieldType en Configdata.kt) y la regla ProGuard de enums (ya presente).
val jsonWithUnknownKeys = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// Para PERSISTIR: con defaults en las propiedades, el Json por defecto (encodeDefaults=false)
// omitiría los campos que coincidan con el default y una versión anterior de la app no
// sabría decodificarlos (MissingFieldException → reset de config en un downgrade).
val jsonPersist = Json { encodeDefaults = true }
val generalsettingsKey = stringPreferencesKey("generalsettings")
val doublefieldKey = stringPreferencesKey("doublefieldsettings")
val sextuplefieldKey = stringPreferencesKey("sextuplefieldsettings")
val onefieldKey = stringPreferencesKey("onefieldsettings")
val smartfieldKey = stringPreferencesKey("smartfieldsettings")
val climbfieldKey = stringPreferencesKey("climbfieldsettings")
val powerKey = stringPreferencesKey("powersettings")
val wprimeBalanceKey = stringPreferencesKey("wprimebalancesettings")
val rollingTimesKey = stringPreferencesKey("rollingtimes")


suspend fun savePowerSettings(context: Context, settings: powerSettings) {

    context.dataStore.edit { t ->
        t[powerKey] = jsonPersist.encodeToString(settings)
    }
}
fun Context.streamStoredPowerSettings(): Flow<powerSettings> {
    return dataStore.data.map { settingsJson ->
        try {
            jsonWithUnknownKeys.decodeFromString<powerSettings>(
                settingsJson[powerKey] ?: defaultPowerSettings)
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read power settings")
            powerSettings()
        }
    }.distinctUntilChanged()
}


suspend fun saveGeneralSettings(context: Context, settings: GeneralSettings) {

    context.dataStore.edit { t ->
        t[generalsettingsKey] = jsonPersist.encodeToString(settings)
    }
}

fun Context.streamGeneralSettings(): Flow<GeneralSettings> {

    return dataStore.data.map { settingsJson ->
        try {
            jsonWithUnknownKeys.decodeFromString<GeneralSettings>(
                settingsJson[generalsettingsKey] ?: defaultGeneralSettings
            )
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString< GeneralSettings>(defaultGeneralSettings)
        }
    }.distinctUntilChanged()
}

suspend fun saveDoubleFieldSettings(context: Context, settings: List<DoubleFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[doublefieldKey] = jsonPersist.encodeToString(settings)
    }
}
fun Context.streamDoubleFieldSettings(): Flow<List<DoubleFieldSettings>> {
    return dataStore.data.map { settingsJson ->
        try {
            val decodedSettings = if (settingsJson.contains(doublefieldKey)) {
                jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(
                    settingsJson[doublefieldKey] ?: defaultDoubleFieldSettings
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(defaultDoubleFieldSettings)
            }

            if (decodedSettings.size == 5) {
                decodedSettings + DoubleFieldSettings(
                    index = 5
                )
            } else {
                decodedSettings
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(defaultDoubleFieldSettings)
        }
    }.distinctUntilChanged()
}

suspend fun saveSextupleFieldSettings(context: Context, settings: List<SextupleFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[sextuplefieldKey] = jsonPersist.encodeToString(settings)
    }
}
fun Context.streamSextupleFieldSettings(): Flow<List<SextupleFieldSettings>> {
    return dataStore.data.map { settingsJson ->
        try {
            val decodedSettings = if (settingsJson.contains(sextuplefieldKey)) {
                jsonWithUnknownKeys.decodeFromString<List<SextupleFieldSettings>>(
                    settingsJson[sextuplefieldKey] ?: defaultSextupleFieldSettings
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<List<SextupleFieldSettings>>(defaultSextupleFieldSettings)
            }

            decodedSettings
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString<List<SextupleFieldSettings>>(defaultSextupleFieldSettings)
        }
    }.distinctUntilChanged()
}


suspend fun saveClimbFieldSettings(context: Context, settings: List<ClimbFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[climbfieldKey] = jsonPersist.encodeToString(settings)
    }
}
fun Context.streamClimbFieldSettings(): Flow<List<ClimbFieldSettings>> {
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson.contains(climbfieldKey)) {
                jsonWithUnknownKeys.decodeFromString<List<ClimbFieldSettings>>(
                    settingsJson[climbfieldKey] ?: defaultClimbFieldSettings
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<List<ClimbFieldSettings>>(defaultClimbFieldSettings)
            }

        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences Climb")
            jsonWithUnknownKeys.decodeFromString<List<ClimbFieldSettings>>(defaultClimbFieldSettings)
        }
    }.distinctUntilChanged()
}
suspend fun saveWPrimeBalanceSettings(context: Context, settings: WPrimeBalanceSettings) {
    context.dataStore.edit { t ->
        t[wprimeBalanceKey] = jsonPersist.encodeToString(settings)
    }
}

fun Context.streamWPrimeBalanceSettings(): Flow<WPrimeBalanceSettings> {
    return dataStore.data.map { settingsJson ->
        try {
            jsonWithUnknownKeys.decodeFromString<WPrimeBalanceSettings>(
                settingsJson[wprimeBalanceKey] ?: defaultWPrimeBalanceSettings)
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read W' Balance settings")
            WPrimeBalanceSettings()
        }
    }.distinctUntilChanged()
}

suspend fun saveOneFieldSettings(context: Context, settings: List<OneFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[onefieldKey] = jsonPersist.encodeToString(settings)
    }
}


fun Context.streamOneFieldSettings(): Flow<List<OneFieldSettings>> {
    return dataStore.data.map { settingsJson ->
        try {
            val decodedSettings = if (settingsJson.contains(onefieldKey)) {
                jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(
                    settingsJson[onefieldKey] ?: defaultOneFieldSettings
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(defaultOneFieldSettings)
            }

            if (decodedSettings.size == 2) {
                decodedSettings + OneFieldSettings(
                    index = 2
                )
            } else {
                decodedSettings
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read OneFieldpreferences")
            jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(defaultOneFieldSettings)
        }
    }.distinctUntilChanged()
}

suspend fun saveSmartFieldSettings(context: Context, settings: List<SmartFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[smartfieldKey] = jsonPersist.encodeToString(settings)
    }
}


fun Context.streamSmartFieldSettings(): Flow<List<SmartFieldSettings>> {
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson.contains(smartfieldKey)) {
                jsonWithUnknownKeys.decodeFromString<List<SmartFieldSettings>>(
                    settingsJson[smartfieldKey] ?: defaultSmartFieldSettings
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<List<SmartFieldSettings>>(defaultSmartFieldSettings)
            }

        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read SmarteFieldpreferences")
            jsonWithUnknownKeys.decodeFromString<List<SmartFieldSettings>>(defaultSmartFieldSettings)
        }
    }.distinctUntilChanged()
}

fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
            trySendBlocking(event.state)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamUserProfile(): Flow<UserProfile> {
    return callbackFlow {
        val listenerId = addConsumer { userProfile: UserProfile ->
            trySendBlocking(userProfile)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

inline fun <reified T : KarooEvent> KarooSystemService.consumerFlow(): Flow<T> {
    return callbackFlow {
        val listenerId = addConsumer<T> {
            trySend(it)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

// Persistencia para rollingTimes
suspend fun saveRollingTimes(context: Context, times: List<RollingTime>) {
    context.dataStore.edit { t ->
        t[rollingTimesKey] = jsonPersist.encodeToString(times)
    }
}

fun Context.streamRollingTimes(): Flow<List<RollingTime>> {
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson.contains(rollingTimesKey)) {
                jsonWithUnknownKeys.decodeFromString<List<RollingTime>>(settingsJson[rollingTimesKey] ?: Json.encodeToString(defaultRollingTimes))
            } else {
                defaultRollingTimes
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read rolling times")
            defaultRollingTimes
        }
    }.distinctUntilChanged()
}
