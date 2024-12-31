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
import kotlin.math.roundToInt
import com.enderthor.kCustomField.R
import com.enderthor.kCustomField.extensions.slopeZones
import com.enderthor.kCustomField.extensions.streamGeneralSettings

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class CustomDoubleTypeBase(
    private val karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : DataTypeImpl(extension, datatype) {
    protected val glance = GlanceRemoteViews()
    protected val context = context.applicationContext

    abstract val leftAction: (CustomFieldSettings) -> KarooAction
    abstract val rightAction: (CustomFieldSettings) -> KarooAction
    abstract val isVertical: (CustomFieldSettings) -> Boolean

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start double type stream")

        val job = CoroutineScope(Dispatchers.IO).launch {
            context.streamSettings()
                .map { settings -> leftAction(settings).action to rightAction(settings).action }
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
                    karooSystem.streamDataFlow(leftAction(settings).action)
                        .combine(karooSystem.streamDataFlow(rightAction(settings).action)) { left: StreamState, right: StreamState -> Quadruple(generalSettings, settings, left, right) }
                        .collect { (generalsettings, settings, left: StreamState, right: StreamState) ->
                            val leftValue = convertValue(left, leftAction(settings).convert, userProfile.preferredUnit.distance)
                            val rightValue = convertValue(right, rightAction(settings).convert, userProfile.preferredUnit.distance)

                            val colorleft = if (leftAction(settings).zone == "heartRateZones") ColorFilter.tint(ColorProvider(Color.Black, Color.Black)) else ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context, leftAction(settings).colorday)), night = Color(ContextCompat.getColor(context, leftAction(settings).colornight))))
                            val colorright = if (rightAction(settings).zone == "heartRateZones") ColorFilter.tint(ColorProvider(Color.Black, Color.Black)) else ColorFilter.tint(ColorProvider(day = Color(ContextCompat.getColor(context, rightAction(settings).colorday)), night = Color(ContextCompat.getColor(context, rightAction(settings).colornight))))

                            val colorzoneleft = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (leftAction(settings).zone == "heartRateZones") userProfile.heartRateZones else if (leftAction(settings).zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource } ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (leftAction(settings).zone == "heartRateZones") userProfile.heartRateZones else if (leftAction(settings).zone == "powerZones") userProfile.powerZones else slopeZones,
                                    leftValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource } ?: R.color.zone7))
                            ).takeIf { leftAction(settings).zone == "heartRateZones" } ?: ColorProvider(Color.White, Color.Black)

                            val colorzoneright = ColorProvider(
                                day = Color(ContextCompat.getColor(context, getZone(
                                    if (rightAction(settings).zone == "heartRateZones") userProfile.heartRateZones else if (rightAction(settings).zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource } ?: R.color.zone7)),
                                night = Color(ContextCompat.getColor(context, getZone(
                                    if (rightAction(settings).zone == "heartRateZones") userProfile.heartRateZones else if (rightAction(settings).zone == "powerZones") userProfile.powerZones else slopeZones,
                                    rightValue
                                )?.let { if (generalsettings.ispalettezwift) it.colorZwift else it.colorResource } ?: R.color.zone7))
                            ).takeIf { rightAction(settings).zone == "heartRateZones" } ?: ColorProvider(Color.White, Color.Black)

                            val result = glance.compose(context, DpSize.Unspecified) {
                                DoubleTypesScreen(leftValue.roundToInt(), rightValue.roundToInt(), leftAction(settings).icon, rightAction(settings).icon, colorleft, colorright, isVertical(settings), colorzoneleft, colorzoneright, config.gridSize.second > 18, karooSystem.hardwareType == HardwareType.KAROO, generalsettings.iscenteralign)
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