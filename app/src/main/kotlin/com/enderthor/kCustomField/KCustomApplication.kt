package com.enderthor.kCustomField

import android.app.Application
import android.util.Log
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant
import timber.log.Timber.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.enderthor.kCustomField.datatype.initializeAntAdvancedPowerProvider
import io.hammerhead.karooext.KarooSystemService


class KCustomApplication : Application() {

    // CoroutineScope global para la aplicación
    val applicationScope = CoroutineScope(SupervisorJob())

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
                  return priority > Log.WARN
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
        Timber.d("Starting KCustom App with ANT+ Advanced Power Meter support")

        // Inicializar el proveedor ANT+ cuando la aplicación arranque
        initializeAntSystem()
    }

    private fun initializeAntSystem() {
        try {
            // Nota: KarooSystemService necesita ser obtenido desde el contexto de extensión
            // La inicialización real se hará en la extensión de Karoo
            Timber.d("ANT+ system initialization prepared")
        } catch (e: Exception) {
            Timber.e(e, "Error preparing ANT+ system")
        }
    }
}
