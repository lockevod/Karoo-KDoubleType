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
import com.enderthor.kCustomField.extensions.getZone
import kotlinx.coroutines.flow.first
import timber.log.Timber
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.slopeZones

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class CustomDoubleVerticalType1(
    private val karooSystem: KarooSystemService,
    extension: String,
    context: Context
) : DataTypeImpl(extension, "vertical-one") {
    private val glance = GlanceRemoteViews()
    private val context = context.applicationContext


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")

        val job = CoroutineScope(Dispatchers.IO).launch {

            context.streamSettings()
                .map { settings -> settings.customverticalleft1.action to settings.customverticalright1.action }
                .collect { (leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> left to right }
                        .collect { (left: StreamState, right: StreamState) ->
                            val leftValue = if (left is StreamState.Streaming) left.dataPoint.singleValue!! else 0.0
                            val rightValue = if (right is StreamState.Streaming) right.dataPoint.singleValue!! else 0.0

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
        fun convertValue(streamState: StreamState, convert: String, unitType: UserProfile.PreferredUnit.UnitType): Double {
            val value = if (streamState is StreamState.Streaming) streamState.dataPoint.singleValue!! else 0.0
            return when (convert) {
                "distance", "speed" -> when (unitType) {
                    UserProfile.PreferredUnit.UnitType.METRIC -> if (convert == "distance") (value / 1000) else (value * 18 / 5)
                    UserProfile.PreferredUnit.UnitType.IMPERIAL -> if (convert == "distance") (value / 1609.345) else (value * 0.0568182)
                }
                else -> value
            }
        }

        val job = CoroutineScope(Dispatchers.IO).launch {

            val userProfile = karooSystem.consumerFlow<UserProfile>().first()

            context.streamSettings()
                .map { setting-> Triple(setting, setting.customverticalleft1.action, setting.customverticalright1.action) }
                .collect { (setting, leftAction, rightAction) ->
                    karooSystem.streamDataFlow(leftAction)
                        .combine(karooSystem.streamDataFlow(rightAction)) { left: StreamState, right: StreamState -> Triple(setting, left, right) }
                        .collect { (settings, left: StreamState, right: StreamState) ->

                            val leftValue = convertValue(left, settings.customverticalleft1.convert, userProfile.preferredUnit.distance)
                            val rightValue = convertValue(right, settings.customverticalright1.convert, userProfile.preferredUnit.distance)

                            val colorleft= ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context, settings.customverticalleft1.colorday)), night = Color(ContextCompat.getColor(context, settings.customverticalleft1.colornight))))
                            val colorright= ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context,settings.customverticalright1.colorday)), night = Color(ContextCompat.getColor(context,settings.customverticalright1.colornight))))

                            val colorzoneleft = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalleft1.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalleft1.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.colorResource ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalleft1.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalleft1.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.colorResource ?: R.color.zone7))
                            ).takeIf { settings.customverticalleft1zone == true } ?: ColorProvider(Color.White, Color.Black)

                            val colorzoneright = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalright1.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalright1.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.colorResource ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalright1.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalright1.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.colorResource ?: R.color.zone7))
                            ).takeIf { settings.customverticalright1zone == true } ?: ColorProvider(Color.White, Color.Black)


                            //Timber.d("Updating view  with LEFT Action $leftAction and values $temp and $leftValue  RIGHT action $rightAction and values $temp2 and $rightValue")
                            //Timber.d("Updating view  with vertical Field 1 is ${settings.ishorizontal1}")


                            val result = glance.compose(context, DpSize.Unspecified) {
                                DoubleTypesVerticalScreen(leftValue, rightValue, settings.customverticalleft1.icon, settings.customverticalright1.icon,colorleft,colorright, settings.ishorizontal1,colorzoneleft,colorzoneright,config.gridSize.second > 18,karooSystem.hardwareType == HardwareType.KAROO, !(settings.customverticalleft1.convert == "speed" || settings.customverticalleft1.zone=="slopeZones"),!(settings.customverticalright1.convert == "speed" || settings.customverticalright1.zone=="slopeZones"))
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
