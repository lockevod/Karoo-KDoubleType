package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enderthor.kCustomField.datatype.ClimbFieldSettings

import com.enderthor.kCustomField.datatype.DoubleFieldSettings
import com.enderthor.kCustomField.datatype.GeneralSettings
import com.enderthor.kCustomField.datatype.OneFieldSettings
import com.enderthor.kCustomField.datatype.SmartFieldSettings
import com.enderthor.kCustomField.datatype.defaultClimbFieldSettings
import com.enderthor.kCustomField.datatype.defaultDoubleFieldSettings
import com.enderthor.kCustomField.datatype.defaultGeneralSettings
import com.enderthor.kCustomField.datatype.defaultOneFieldSettings
import com.enderthor.kCustomField.datatype.defaultSmartFieldSettings
import com.enderthor.kCustomField.datatype.defaultPowerSettings
import com.enderthor.kCustomField.datatype.powerSettings

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import timber.log.Timber

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }
val generalsettingsKey = stringPreferencesKey("generalsettings")
val doublefieldKey = stringPreferencesKey("doublefieldsettings")
val onefieldKey = stringPreferencesKey("onefieldsettings")
val smartfieldKey = stringPreferencesKey("smartfieldsettings")
val climbfieldKey = stringPreferencesKey("climbfieldsettings")
val powerKey = stringPreferencesKey("powersettings")


suspend fun savePowerSettings(context: Context, settings: powerSettings) {

    context.dataStore.edit { t ->
        t[powerKey] = Json.encodeToString(settings)
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

fun KarooSystemService.streamPowerSettings(context: Context): Flow<Pair<powerSettings, Double>> {
    return combine(
        context.streamStoredPowerSettings(),
        streamUserProfile()
    ) { powerSettings, userProfile ->
        Pair(powerSettings, userProfile.weight.toDouble())
    }
}

suspend fun saveGeneralSettings(context: Context, settings: GeneralSettings) {

    context.dataStore.edit { t ->
        t[generalsettingsKey] = Json.encodeToString(settings)
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
        t[doublefieldKey] = Json.encodeToString(settings)
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


suspend fun saveClimbFieldSettings(context: Context, settings: List<ClimbFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[climbfieldKey] = Json.encodeToString(settings)
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
suspend fun saveOneFieldSettings(context: Context, settings: List<OneFieldSettings>) {
    // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[onefieldKey] = Json.encodeToString(settings)
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
        t[smartfieldKey] = Json.encodeToString(settings)
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
    return callbackFlow<StreamState> {
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