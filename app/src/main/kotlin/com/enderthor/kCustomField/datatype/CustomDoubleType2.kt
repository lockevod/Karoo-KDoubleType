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
import timber.log.Timber

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class CustomDoubleType2(
    private val karooSystem: KarooSystemService,
    extension: String,
    context: Context
) : DataTypeImpl(extension, "custom-two") {
    private val glance = GlanceRemoteViews()
    private val context = context.applicationContext


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
            context.streamSettings()
                .map { settings -> settings.customleft2.action to settings.customright2.action }
                .collect { (leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> left to right }
                        .collect { (left: StreamState, right: StreamState) ->
                            val leftValue = if (left is StreamState.Streaming) left.dataPoint.singleValue!! else 0.0
                            val rightValue = if (right is StreamState.Streaming) right.dataPoint.singleValue!! else 0.0
                                   // val value = leftValue + rightValue

                            Timber.d("Updating stream with $leftValue and $rightValue")
                            emitter.onNext(
                                StreamState.Streaming(
                                    DataPoint(
                                        dataTypeId,
                                        mapOf(DataType.Field.SINGLE to leftValue, DataType.Field.SINGLE to rightValue)
                                    )
                                )
                            )
                        }
                }
        }
        emitter.setCancellable {
            Timber.d("stop double type stream")
            job.cancel()
        }
    }
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val configJob = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            awaitCancellation()
        }
        Timber.d("Starting double type view with $emitter and config $config")

        val job = CoroutineScope(Dispatchers.IO).launch {
            context.streamSettings()
                .map { settings -> Triple(settings, settings.customleft2.action, settings.customright2.action) }
                .collect { (settings, leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> Triple(settings, left, right) }
                        .collect { (settings, left: StreamState, right: StreamState) ->
                            val leftValue = if (left is StreamState.Streaming) left.dataPoint.singleValue!!.toInt() % 1000 else 0
                            val rightValue = if (right is StreamState.Streaming) right.dataPoint.singleValue!!.toInt() % 1000 else 0

                            Timber.d("Updating view ($emitter) with $leftValue and $rightValue")
                            val result = glance.compose(context, DpSize.Unspecified) {
                                NumberWithIcon(leftValue, rightValue, settings.customleft2.icon, settings.customright2.icon)
                            }
                            emitter.updateView(result.remoteViews)
                        }
                }
        }

        emitter.setCancellable {
            Timber.d("Stopping speed view with $emitter")
            configJob.cancel()
            job.cancel()
        }
    }
}
