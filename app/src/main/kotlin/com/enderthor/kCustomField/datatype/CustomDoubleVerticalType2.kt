package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.color.ColorProvider
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.getZone
import com.enderthor.kCustomField.extensions.slopeZones
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import com.enderthor.kCustomField.extensions.streamSettings
import com.enderthor.kCustomField.extensions.streamDataFlow
import com.enderthor.kCustomField.extensions.streamGeneralSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class CustomDoubleVerticalType2(
    private val karooSystem: KarooSystemService,
    extension: String,
    context: Context
) : DataTypeImpl(extension, "vertical-two") {
    private val glance = GlanceRemoteViews()
    private val context = context.applicationContext


    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")

        val job = CoroutineScope(Dispatchers.IO).launch {
            context.streamSettings()
                .map { settings -> settings.customverticalleft2.action to settings.customverticalright2.action }
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
        Timber.d("Starting double type view with $emitter and config $config")

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
                .combine(context.streamGeneralSettings()) { settings, generalSettings ->
                    settings to generalSettings
                }
                .collect { (settings, generalSettings) ->
                    karooSystem.streamDataFlow(settings.customverticalleft2.action)
                        .combine(karooSystem.streamDataFlow(settings.customverticalright2.action)) { left: StreamState, right: StreamState -> Quadruple(generalSettings, settings,left, right) }
                        .collect { (generalsettings, settings, left: StreamState, right: StreamState) ->

                        val leftValue = convertValue(left, settings.customverticalleft2.convert, userProfile.preferredUnit.distance)
                            val rightValue = convertValue(right, settings.customverticalright2.convert, userProfile.preferredUnit.distance)

                            val colorleft= if(settings.customverticalleft2zone == true) ColorFilter.tint(ColorProvider(Color.Black, Color.Black)) else ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context, settings.customverticalleft2.colorday)), night = Color(ContextCompat.getColor(context, settings.customverticalleft2.colornight))))
                            val colorright= if(settings.customverticalright2zone == true) ColorFilter.tint(ColorProvider(Color.Black, Color.Black)) else ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context,settings.customverticalright2.colorday)), night = Color(ContextCompat.getColor(context,settings.customverticalright2.colornight))))

                            val colorzoneleft = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalleft2.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalleft2.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource} ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalleft2.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalleft2.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource} ?: R.color.zone7))
                            ).takeIf { settings.customverticalleft2zone == true } ?: ColorProvider(Color.White, Color.Black)

                            val colorzoneright = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalright2.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalright2.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource} ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (settings.customverticalright2.zone == "heartRateZones") userProfile.heartRateZones else if (settings.customverticalright2.zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource} ?: R.color.zone7))
                            ).takeIf { settings.customverticalright2zone == true } ?: ColorProvider(Color.White, Color.Black)


                            //Timber.d("Updating view ($emitter) with $leftValue and $rightValue")
                            val result = glance.compose(context, DpSize.Unspecified) {
                                DoubleTypesVerticalScreen(leftValue, rightValue, settings.customverticalleft2.icon, settings.customverticalright2.icon,colorleft,colorright,settings.ishorizontal2,colorzoneleft,colorzoneright,config.gridSize.second > 18,karooSystem.hardwareType == HardwareType.KAROO,!(settings.customverticalleft2.convert == "speed" || settings.customverticalleft2.zone=="slopeZones"),!(settings.customverticalright2.convert == "speed" || settings.customverticalright2.zone=="slopeZones"),generalsettings.iscentervertical)
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
