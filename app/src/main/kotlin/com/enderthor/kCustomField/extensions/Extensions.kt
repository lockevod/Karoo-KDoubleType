package com.enderthor.kCustomField.extensions

import android.content.Context

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey


import com.enderthor.kCustomField.datatype.DoubleFieldSettings

import com.enderthor.kCustomField.datatype.GeneralSettings


import com.enderthor.kCustomField.datatype.OneFieldSettings

import com.enderthor.kCustomField.datatype.defaultDoubleFieldSettings
import com.enderthor.kCustomField.datatype.defaultGeneralSettings

import com.enderthor.kCustomField.datatype.defaultOneFieldSettings


import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
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
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import timber.log.Timber

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }
val generalsettingsKey = stringPreferencesKey("generalsettings")
val doublefieldKey = stringPreferencesKey("doublefieldsettings")
val onefieldKey = stringPreferencesKey("onefieldsettings")


suspend fun saveGeneralSettings(context: Context, settings: GeneralSettings) {

    context.dataStore.edit { t ->
        t[generalsettingsKey] = Json.encodeToString(settings)
    }
}

fun Context.streamGeneralSettings(): Flow<GeneralSettings> {
   // Timber.d("streamSettings IN")
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
  //  Timber.d("streamSettings DoubleField IN")
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson.contains(doublefieldKey)) {

               jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(
                    settingsJson[doublefieldKey] ?: defaultDoubleFieldSettings
                )

            } else {
                jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(defaultDoubleFieldSettings)
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString<List<DoubleFieldSettings>>(defaultDoubleFieldSettings)
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
    //  Timber.d("streamSettings OneField IN")
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson.contains(onefieldKey)) {

                 jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(
                    settingsJson[onefieldKey] ?: defaultOneFieldSettings)

            }  else {
                jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(defaultOneFieldSettings)
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read OneFieldpreferences")
            jsonWithUnknownKeys.decodeFromString<List<OneFieldSettings>>(defaultOneFieldSettings)
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
    }.onStart {
            emit(StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to 0.0), ""))) }
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

