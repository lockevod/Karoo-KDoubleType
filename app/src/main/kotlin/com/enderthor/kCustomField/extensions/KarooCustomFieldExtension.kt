package com.enderthor.kCustomField.extensions


import com.enderthor.kCustomField.datatype.CustomDoubleType1
import com.enderthor.kCustomField.datatype.CustomDoubleType2
import com.enderthor.kCustomField.datatype.CustomFieldSettings

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import timber.log.Timber

class KarooCustomFieldExtension : KarooExtension("kcustomfield", "1.0-beta") {

    lateinit var karooSystem: KarooSystemService
    private var serviceJob: Job? = null

   override val types by lazy {
        listOf(
            CustomDoubleType1(karooSystem, extension, applicationContext),
            CustomDoubleType2(karooSystem, extension, applicationContext),

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
            CoroutineScope(Dispatchers.IO).launch {
                saveSettings(applicationContext, CustomFieldSettings())
            }
        }
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }
}
