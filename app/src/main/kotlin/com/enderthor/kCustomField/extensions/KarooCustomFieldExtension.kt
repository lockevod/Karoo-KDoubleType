package com.enderthor.kCustomField.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.enderthor.kCustomField.datatype.CustomDoubleType
import com.enderthor.kCustomField.datatype.CustomRollingType

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.HardwareType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import timber.log.Timber

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class KarooCustomFieldExtension : KarooExtension("kcustomfield", "2.0") {

    lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null
    private var _types: List<DataTypeImpl> = emptyList()

    override val types: List<DataTypeImpl>
        get() = _types


    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(applicationContext)

        Timber.d("Service created")

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                        Timber.d("Connected to Karoo system")
                        _types = if (karooSystem.hardwareType == HardwareType.KAROO) {
                            listOf(
                                CustomDoubleType(karooSystem, extension, "custom-one", 0),
                                CustomDoubleType(karooSystem, extension, "custom-two", 1),
                                CustomDoubleType(karooSystem, extension, "custom-three", 2),
                                CustomDoubleType(karooSystem, extension, "vertical-one", 3),
                                CustomDoubleType(karooSystem, extension, "vertical-two", 4),
                                CustomRollingType(karooSystem, extension, "rolling-one", 0),
                                CustomRollingType(karooSystem, extension, "rolling-two", 1)
                            )
                        } else {
                            listOf(
                                CustomDoubleType(karooSystem, extension, "custom-one", 0),
                                CustomDoubleType(karooSystem, extension, "custom-two", 1),
                                CustomDoubleType(karooSystem, extension, "custom-three", 2),
                                CustomRollingType(karooSystem, extension, "rolling-one", 0)
                            )
                        }


                    }
            }
        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
