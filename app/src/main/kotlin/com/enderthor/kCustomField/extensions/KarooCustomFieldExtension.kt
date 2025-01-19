package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.enderthor.kCustomField.datatype.CustomDoubleType
import com.enderthor.kCustomField.datatype.CustomRollingType

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
       /* listOf(
            "custom-one",
            "custom-two",
            "custom-three",
            "vertical-one",
            "vertical-two",
            "vertical-three",
            "rolling-one",
            "rolling-two",
            "rolling-three"
        ).mapIndexed { index, name ->
            if (name.contains("custom") || name.contains("vertical")) {
                CustomDoubleType(karooSystem, extension, name, index)
            } else {
                CustomRollingType(karooSystem, extension, name, index - 6)
            }
        }*/
        listOf(
            CustomDoubleType(karooSystem, extension, "custom-one",applicationContext, 0),
            CustomDoubleType(karooSystem, extension, "custom-two", applicationContext,1),
            CustomDoubleType(karooSystem, extension, "custom-three",applicationContext, 2),
            CustomDoubleType(karooSystem, extension, "vertical-one",applicationContext, 3),
            CustomDoubleType(karooSystem, extension, "vertical-two", applicationContext,4),
            CustomDoubleType(karooSystem, extension, "vertical-three",applicationContext, 5),
            CustomRollingType(karooSystem, extension, "rolling-one", applicationContext,0),
            CustomRollingType(karooSystem, extension, "rolling-two",applicationContext, 1),
            CustomRollingType(karooSystem, extension, "rolling-three",applicationContext, 2)
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
