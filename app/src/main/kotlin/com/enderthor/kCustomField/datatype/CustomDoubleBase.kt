package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import com.enderthor.kCustomField.extensions.streamSettings
import com.enderthor.kCustomField.extensions.streamDataFlow
import kotlinx.coroutines.flow.map
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.color.ColorProvider
import com.enderthor.kCustomField.extensions.consumerFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.math.roundToInt

abstract class CustomDoubleBase(
    private val applicationContext: Context,
    dataTypeId: String
) : DataTypeImpl("karoo-headwind", dataTypeId) {
    abstract fun getValue(data: de.timklge.karooheadwind.OpenMeteoCurrentWeatherResponse): Double

    override fun startStream(emitter: Emitter<StreamState>) {
        Log.d(de.timklge.karooheadwind.KarooHeadwindExtension.Companion.TAG, "start ${DataTypeImpl.dataTypeId} stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
            val currentWeatherData = applicationContext.streamCurrentWeatherData()

            Flow.collect { data ->
                val value = getValue(data)
                Log.d(
                    de.timklge.karooheadwind.KarooHeadwindExtension.Companion.TAG,
                    "${DataTypeImpl.dataTypeId}: $value"
                )
                Emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            DataTypeImpl.dataTypeId,
                            mapOf(DataType.Field.SINGLE to value)
                        )
                    )
                )
            }
        }
        Emitter.setCancellable {
            Log.d(
                de.timklge.karooheadwind.KarooHeadwindExtension.Companion.TAG,
                "stop ${DataTypeImpl.dataTypeId} stream"
            )
            Job.cancel()
        }
    }
}
