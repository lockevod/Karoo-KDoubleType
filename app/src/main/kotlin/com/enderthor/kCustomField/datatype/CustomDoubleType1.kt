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

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class CustomDoubleType1(
    private val karooSystem: KarooSystemService,
    extension: String,
    context: Context
) : DataTypeImpl(extension, "custom-one") {
    private val glance = GlanceRemoteViews()
    private val context = context.applicationContext


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")

        val job = CoroutineScope(Dispatchers.IO).launch {

            context.streamSettings()
                .map { settings -> settings.customleft1.action to settings.customright1.action }
                .collect { (leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> left to right }
                        .collect { (left: StreamState, right: StreamState) ->
                            val leftValue = if (left is StreamState.Streaming) left.dataPoint.singleValue!! else 0.0
                            val rightValue = if (right is StreamState.Streaming) right.dataPoint.singleValue!! else 0.0
                                   // val value = leftValue + rightValue

                            //Timber.d("Updating stream with $leftValue and $rightValue")
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
       // Timber.d("Starting double type view with $emitter and config $config")
        fun convertValue(streamState: StreamState, convert: String, unitType: UserProfile.PreferredUnit.UnitType): Int {
            val value = if (streamState is StreamState.Streaming) streamState.dataPoint.singleValue!! else 0.0
            return when (convert) {
                "distance", "speed" -> when (unitType) {
                    UserProfile.PreferredUnit.UnitType.METRIC -> if (convert == "distance") (value / 1000).roundToInt() else (value * 18 / 5).roundToInt()
                    UserProfile.PreferredUnit.UnitType.IMPERIAL -> if (convert == "distance") (value / 1609.345).roundToInt() else (value * 0.0568182).roundToInt()
                }
                else -> value.roundToInt()
            }
        }

        val job = CoroutineScope(Dispatchers.IO).launch {

            val userProfile = karooSystem.consumerFlow<UserProfile>().first()

            context.streamSettings()
                .map { setting-> Triple(setting, setting.customleft1.action, setting.customright1.action) }
                .collect { (setting, leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> Triple(setting, left, right) }
                        .collect { (settings, left: StreamState, right: StreamState) ->


                            val leftValue = convertValue(left, settings.customleft1.convert, userProfile.preferredUnit.distance)
                            val rightValue = convertValue(right, settings.customright1.convert, userProfile.preferredUnit.distance)


                            val colorleft= ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context, settings.customleft1.colorday)), night = Color(ContextCompat.getColor(context, settings.customleft1.colornight))))
                            val colorright= ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context,settings.customright1.colorday)), night = Color(ContextCompat.getColor(context,settings.customright1.colornight))))


                            //Timber.d("Updating view  with LEFT Action $leftAction and values $temp and $leftValue  RIGHT action $rightAction and values $temp2 and $rightValue")
                            Timber.d("Updating view  with vertical Field 1 is ${settings.isvertical1}")
                            val result = glance.compose(context, DpSize.Unspecified) {
                                NumberWithIcon(leftValue, rightValue, settings.customleft1.icon, settings.customright1.icon,colorleft,colorright, settings.isvertical1)
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
