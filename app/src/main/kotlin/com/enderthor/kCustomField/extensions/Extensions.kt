package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enderthor.kCustomField.datatype.CustomFieldSettings

import com.enderthor.kCustomField.datatype.DoubleFieldSettings
import com.enderthor.kCustomField.datatype.DoubleFieldType
import com.enderthor.kCustomField.datatype.GeneralSettings
import com.enderthor.kCustomField.datatype.defaultDoubleFieldSettings
import com.enderthor.kCustomField.datatype.defaultGeneralSettings
import com.enderthor.kCustomField.datatype.defaultSettings

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }
val settingsKey = stringPreferencesKey("settings")
val generalsettingsKey = stringPreferencesKey("generalsettings")
val doublefieldKey = stringPreferencesKey("doublefieldsettings")


suspend fun saveGeneralSettings(context: Context, settings: GeneralSettings) {
    Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[generalsettingsKey] = Json.encodeToString(settings)
    }
}


fun Context.streamGeneralSettings(): Flow<GeneralSettings> {
    Timber.d("streamSettings IN")
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
    Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[doublefieldKey] = Json.encodeToString(settings)
    }
}


fun Context.streamDoubleFieldSettings(): Flow<MutableList<DoubleFieldSettings>> {
    Timber.d("streamSettings DoubleField IN")
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson[doublefieldKey] != null) {
                jsonWithUnknownKeys.decodeFromString<MutableList<DoubleFieldSettings>>(
                    settingsJson[doublefieldKey] ?: defaultDoubleFieldSettings
                )
            } else if (settingsJson[settingsKey] != null) {
                val customSettings = jsonWithUnknownKeys.decodeFromString<CustomFieldSettings>(
                    settingsJson[settingsKey] ?: defaultSettings
                )
                mutableListOf(
                    DoubleFieldSettings(0, DoubleFieldType(customSettings.customleft1, customSettings.customleft1zone), DoubleFieldType(customSettings.customright1, customSettings.customright1zone),true,true),
                    DoubleFieldSettings(1, DoubleFieldType(customSettings.customleft2, customSettings.customleft2zone), DoubleFieldType(customSettings.customright2, customSettings.customright2zone),true,true),
                    DoubleFieldSettings(2, DoubleFieldType(customSettings.customleft3, customSettings.customleft3zone,), DoubleFieldType(customSettings.customright3, customSettings.customright3zone),true,true),
                    DoubleFieldSettings(3, DoubleFieldType(customSettings.customverticalleft1, customSettings.customverticalleft1zone), DoubleFieldType(customSettings.customverticalright1, customSettings.customright1zone),false,true),
                    DoubleFieldSettings(4, DoubleFieldType(customSettings.customverticalleft2, customSettings.customverticalleft2zone), DoubleFieldType(customSettings.customverticalright2, customSettings.customright2zone),false,true),
                    DoubleFieldSettings(5, DoubleFieldType(customSettings.customverticalleft3, customSettings.customverticalleft3zone), DoubleFieldType(customSettings.customverticalright3, customSettings.customright3zone),false,true)
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<MutableList<DoubleFieldSettings>>(defaultDoubleFieldSettings)
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString<MutableList<DoubleFieldSettings>>(defaultDoubleFieldSettings)
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