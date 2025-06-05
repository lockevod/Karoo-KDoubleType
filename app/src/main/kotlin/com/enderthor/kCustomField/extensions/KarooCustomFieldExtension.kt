package com.enderthor.kCustomField.extensions

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

import com.enderthor.kCustomField.BuildConfig
import com.enderthor.kCustomField.datatype.BellActionDataType
import com.enderthor.kCustomField.datatype.CustomClimbType
import com.enderthor.kCustomField.datatype.CustomDoubleType
import com.enderthor.kCustomField.datatype.CustomRollingType


import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


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