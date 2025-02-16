package com.enderthor.kCustomField.extensions

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.hammerhead.karooext.KarooSystemService

import io.hammerhead.karooext.extension.KarooExtension

import com.enderthor.kCustomField.BuildConfig
import com.enderthor.kCustomField.datatype.CustomDoubleType
import com.enderthor.kCustomField.datatype.CustomRollingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class KarooCustomFieldExtension : KarooExtension("kcustomfield", BuildConfig.VERSION_NAME) {

    lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null

    private val extensionIdentifier = "kcustomfield"
    val extensionId: String get() = extensionIdentifier




    private val scope = CoroutineScope(Dispatchers.IO)

    override val types by lazy {
        listOf(
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "custom-one", 0) ,
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "custom-two", 1) ,
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "custom-three", 2) ,
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "vertical-one", 3) ,
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "vertical-two", 4) ,
            CustomDoubleType(karooSystem, this@KarooCustomFieldExtension, "vertical-three", 5) ,
            CustomRollingType(karooSystem, extension, "rolling-one", 0),
            CustomRollingType(karooSystem, extension, "rolling-two", 1),
            CustomRollingType(karooSystem, extension, "rolling-three", 2)
        )
    }


    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(applicationContext)

        Timber.d("Service KDouble created")

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    Timber.d("Connected to Karoo system")
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        serviceJob?.cancel()
        karooSystem.disconnect()
        super.onDestroy()
    }
}