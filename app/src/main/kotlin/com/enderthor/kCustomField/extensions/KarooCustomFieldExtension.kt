package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.enderthor.kCustomField.datatype.CustomDoubleType1
import com.enderthor.kCustomField.datatype.CustomDoubleType2
import com.enderthor.kCustomField.datatype.CustomDoubleVerticalType1
//import com.enderthor.kCustomField.datatype.CustomDoubleType2


import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class KarooCustomFieldExtension : KarooExtension("kcustomfield", "1.3") {

    lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null

   override val types by lazy {
        listOf(
            CustomDoubleType1(karooSystem, extension, applicationContext),
            CustomDoubleType2(karooSystem, extension, applicationContext),
            CustomDoubleVerticalType1(karooSystem, extension, applicationContext),
           // CustomDoubleVerticalType2(karooSystem, extension, applicationContext),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(applicationContext)

        Timber.d("Service created")

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    Timber.d("Connected to Karoo system")
                }
            }

        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
