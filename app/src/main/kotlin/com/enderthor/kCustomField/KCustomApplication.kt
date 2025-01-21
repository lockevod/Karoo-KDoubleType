package com.enderthor.kCustomField

import android.app.Application
import timber.log.Timber


class KCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

       // Timber.plant(Timber.DebugTree())
        Timber.Forest.d("Starting KCustom App")
    }
}
