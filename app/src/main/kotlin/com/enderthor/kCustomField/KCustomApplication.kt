package com.enderthor.kCustomField

import android.app.Application
import android.util.Log
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant
import timber.log.Timber.Tree


class KCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

       if (BuildConfig.DEBUG) {
            plant(object : DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    Log.println(
                        priority,
                        tag,
                        message + (if (t == null) "" else "\n" + t.message + "\n" + Log.getStackTraceString(
                            t
                        ))
                    )
                }
            })
      } else {
          Timber.plant(object : Tree() {
              override fun isLoggable(tag: String?, priority: Int): Boolean {
                  return priority > Log.INFO
              }

              override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                  Log.println(
                      priority,
                      tag,
                      message + (if (t == null) "" else "\n" + t.message + "\n" + Log.getStackTraceString(
                          t
                      ))
                  )
              }

          })
          }
        Timber.d("Starting KCustom App")
    }
}
