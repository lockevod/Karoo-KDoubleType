package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enderthor.kCustomField.datatype.CustomFieldSettings

import com.enderthor.kCustomField.datatype.DoubleFieldSettings
import com.enderthor.kCustomField.datatype.DoubleFieldType
import com.enderthor.kCustomField.datatype.GeneralSettings

import com.enderthor.kCustomField.datatype.KarooAction
import com.enderthor.kCustomField.datatype.OneFieldSettings
import com.enderthor.kCustomField.datatype.OneFieldType
import com.enderthor.kCustomField.datatype.RollingTime
import com.enderthor.kCustomField.datatype.defaultDoubleFieldSettings
import com.enderthor.kCustomField.datatype.defaultGeneralSettings
import com.enderthor.kCustomField.datatype.defaultSettings
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
val settingsKey = stringPreferencesKey("settings")
val generalsettingsKey = stringPreferencesKey("generalsettings")
val doublefieldKey = stringPreferencesKey("doublefieldsettings")
val onefieldKey = stringPreferencesKey("onefieldsettings")


suspend fun saveGeneralSettings(context: Context, settings: GeneralSettings) {
   // Timber.d("saveSettings IN $settings")
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


fun Context.streamDoubleFieldSettings(): Flow<MutableList<DoubleFieldSettings>> {
  //  Timber.d("streamSettings DoubleField IN")
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson[doublefieldKey] != null) {

                val settingsList = jsonWithUnknownKeys.decodeFromString<MutableList<DoubleFieldSettings>>(
                    settingsJson[doublefieldKey] ?: defaultDoubleFieldSettings
                )
                if (settingsList.size < 6) {
                    settingsList.add(DoubleFieldSettings())
                }
                settingsList

            } else if (settingsJson[settingsKey] != null) {
                val customSettings = jsonWithUnknownKeys.decodeFromString<CustomFieldSettings>(
                    settingsJson[settingsKey] ?: defaultSettings
                )

                mutableListOf(
                    DoubleFieldSettings(0, DoubleFieldType(customSettings.customleft1, if( customSettings.customleft1.zone =="none") false else customSettings.customleft1zone), DoubleFieldType(customSettings.customright1, if( customSettings.customright1.zone =="none") false else customSettings.customright1zone),true,true),
                    DoubleFieldSettings(1, DoubleFieldType(customSettings.customleft2, if( customSettings.customleft2.zone =="none") false else customSettings.customleft2zone), DoubleFieldType(customSettings.customright2, if( customSettings.customright2.zone =="none") false else customSettings.customright2zone),true,true),
                    DoubleFieldSettings(2, DoubleFieldType(customSettings.customleft3, if( customSettings.customleft3.zone =="none") false else customSettings.customleft3zone), DoubleFieldType(customSettings.customright3, if( customSettings.customright3.zone =="none") false else customSettings.customright3zone),true,true),
                    DoubleFieldSettings(3, DoubleFieldType(customSettings.customverticalleft1, if(customSettings.customverticalleft1.zone =="none") false else customSettings.customverticalleft1zone), DoubleFieldType(customSettings.customverticalright1, if(customSettings.customverticalright1.zone =="none") false else customSettings.customright1zone),false,true),
                    DoubleFieldSettings(4, DoubleFieldType(customSettings.customverticalleft2, if(customSettings.customverticalleft2.zone =="none") false else customSettings.customverticalleft2zone), DoubleFieldType(customSettings.customverticalright2, if(customSettings.customverticalright2.zone =="none") false else customSettings.customright2zone),false,true),
                    DoubleFieldSettings(5, DoubleFieldType(customSettings.customverticalleft3, if(customSettings.customverticalleft3.zone =="none") false else customSettings.customverticalleft3zone), DoubleFieldType(customSettings.customverticalright3, if(customSettings.customverticalright3.zone =="none") false else customSettings.customright3zone),false,true),
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



suspend fun saveOneFieldSettings(context: Context, settings: List<OneFieldSettings>) {
   // Timber.d("saveSettings IN $settings")
    context.dataStore.edit { t ->
        t[onefieldKey] = Json.encodeToString(settings)
    }
}


fun Context.streamOneFieldSettings(): Flow<MutableList<OneFieldSettings>> {
    //  Timber.d("streamSettings OneField IN")
    return dataStore.data.map { settingsJson ->
        try {
            if (settingsJson[onefieldKey] != null) {

                val settingsList = jsonWithUnknownKeys.decodeFromString<MutableList<OneFieldSettings>>(
                    settingsJson[onefieldKey] ?: defaultOneFieldSettings
                )
                if (settingsList.size < 3) {
                    settingsList.add(OneFieldSettings())
                }
                settingsList


            } else if (settingsJson[settingsKey] != null) {
                val customSettings = jsonWithUnknownKeys.decodeFromString<CustomFieldSettings>(
                    settingsJson[settingsKey] ?: defaultSettings
                )
                mutableListOf(
                    OneFieldSettings(0, OneFieldType(customSettings.customverticalleft3, if(customSettings.customverticalleft3.zone =="none") false else customSettings.customverticalleft3zone,true), OneFieldType(customSettings.customverticalright3, if(customSettings.customverticalright3.zone =="none") false else customSettings.customright3zone,true),OneFieldType(KarooAction.SPEED, false,false), RollingTime("LOW", "5s", 5000L)),
                    OneFieldSettings(1, OneFieldType(KarooAction.SPEED, false, true),OneFieldType(KarooAction.SPEED, false, false),OneFieldType(KarooAction.SPEED, false, false),RollingTime("ZERO", "0s", 0L))
                )
            } else {
                jsonWithUnknownKeys.decodeFromString<MutableList<OneFieldSettings>>(defaultOneFieldSettings)
            }
        } catch (e: Throwable) {
            Timber.tag("KarooDualTypeExtension").e(e, "Failed to read OneFieldpreferences")
            jsonWithUnknownKeys.decodeFromString<MutableList<OneFieldSettings>>(defaultOneFieldSettings)
        }
    }.distinctUntilChanged()
}

fun KarooSystemService.streamDataFlow(dataTypeId: String,period:Long=100L): Flow<StreamState> {
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

