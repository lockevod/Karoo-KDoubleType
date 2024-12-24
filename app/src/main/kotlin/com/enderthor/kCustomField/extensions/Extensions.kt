package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import com.enderthor.kCustomField.datatype.CustomFieldSettings
import com.enderthor.kCustomField.datatype.defaultSettings

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
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


val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }
val settingsKey = stringPreferencesKey("settings")

suspend fun saveSettings(context: Context, settings: CustomFieldSettings) {
    context.dataStore.edit { t ->
        t[settingsKey] = Json.encodeToString(settings)
    }
}

fun Context.streamSettings(karooSystemService: KarooSystemService): Flow<CustomFieldSettings> {
    return dataStore.data.map { settingsJson ->
        try {
            jsonWithUnknownKeys.decodeFromString<CustomFieldSettings>(
                settingsJson[settingsKey] ?: defaultSettings
            )
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read preferences")
            jsonWithUnknownKeys.decodeFromString<CustomFieldSettings>(defaultSettings)
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


fun KarooSystemService.streamRideState(): Flow<RideState> {
    return callbackFlow {
        val listenerId = addConsumer { rideState: RideState ->
            trySendBlocking(rideState)
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