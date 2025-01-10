package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

import com.enderthor.kCustomField.datatype.CustomDoubleType1
import com.enderthor.kCustomField.datatype.CustomDoubleType2
import com.enderthor.kCustomField.datatype.CustomDoubleType3
import com.enderthor.kCustomField.datatype.CustomDoubleType4
import com.enderthor.kCustomField.datatype.CustomDoubleType5
import com.enderthor.kCustomField.datatype.CustomDoubleType6

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class KarooCustomFieldExtension : KarooExtension("kcustomfield", "1.8") {

    lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null

   override val types by lazy {
        listOf(
            CustomDoubleType1(karooSystem, extension, "custom-one"),
            CustomDoubleType2(karooSystem, extension, "custom-two"),
            CustomDoubleType3(karooSystem, extension, "custom-three"),
            CustomDoubleType4(karooSystem, extension,"vertical-one"),
            CustomDoubleType5(karooSystem, extension, "vertical-two"),
            CustomDoubleType6(karooSystem, extension,"vertical-three"),


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
