package com.enderthor.kCustomField

import android.app.Application
import timber.log.Timber


class KCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.plant(Timber.DebugTree())
        Timber.Forest.d("Starting KCustom App")
    }
}
