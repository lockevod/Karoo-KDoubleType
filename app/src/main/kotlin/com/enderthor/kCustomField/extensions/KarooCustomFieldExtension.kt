package com.enderthor.kCustomField.extensions

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

import com.enderthor.kCustomField.BuildConfig
import com.enderthor.kCustomField.datatype.BellActionDataType
import com.enderthor.kCustomField.datatype.CustomClimbType
import com.enderthor.kCustomField.datatype.CustomDoubleType
import com.enderthor.kCustomField.datatype.CustomSextupleType
import com.enderthor.kCustomField.datatype.CustomRollingType


import timber.log.Timber

// Sin corruptionHandler, un kill del proceso a mitad de un edit{} deja settings.preferences_pb
// ilegible y DataStore relanza CorruptionException en CADA lectura, para siempre: los catch de
// cada stream NO lo capturan porque se lanza aguas arriba del map{}, al abrir el fichero. La
// extensión entra en bucle de crash y el usuario solo puede salir borrando los datos de la app.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)


class KarooCustomFieldExtension : KarooExtension("kcustomfield", BuildConfig.VERSION_NAME) {

    lateinit var karooSystem: KarooSystemService

    companion object {
        lateinit var instance: KarooCustomFieldExtension
            private set
    }


    override val types by lazy {
        listOf(
            CustomDoubleType(karooSystem, "custom-one", 0) ,
            CustomDoubleType(karooSystem, "custom-two", 1) ,
            CustomDoubleType(karooSystem,  "custom-three", 2) ,
            CustomDoubleType(karooSystem,  "vertical-one", 3) ,
            CustomDoubleType(karooSystem,  "vertical-two", 4) ,
            CustomDoubleType(karooSystem,  "vertical-three", 5) ,
            CustomSextupleType(karooSystem,  "sextuple-one", 0) ,
            CustomSextupleType(karooSystem,  "sextuple-two", 1) ,
            CustomSextupleType(karooSystem,  "sextuple-three", 2) ,
            CustomRollingType(karooSystem, "rolling-one", 0),
            CustomRollingType(karooSystem,  "rolling-two", 1),
            CustomRollingType(karooSystem,  "rolling-three", 2),
            CustomClimbType(karooSystem,  "climb-one", 0),
            BellActionDataType( "custom-bell")
        )
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        karooSystem = KarooSystemService(applicationContext)

        Timber.d("Service KDouble created")
        karooSystem.connect { connected ->
            if (connected) {
                Timber.d("Connected to Karoo system")
            }
        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}