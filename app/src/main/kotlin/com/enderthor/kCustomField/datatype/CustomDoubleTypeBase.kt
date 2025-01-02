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
import androidx.glance.unit.ColorProvider
import com.enderthor.kCustomField.extensions.consumerFlow
import com.enderthor.kCustomField.extensions.getZone
import kotlinx.coroutines.flow.first
import timber.log.Timber
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
    abstract val leftZone:  (CustomFieldSettings) -> Boolean
    abstract val rightZone:  (CustomFieldSettings) -> Boolean
    abstract val showh: Boolean



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

        fun getColorZone(context: Context, zone: String, value: Double, userProfile: UserProfile, isPaletteZwift: Boolean): ColorProvider {
            val zoneData = when (zone) {
                "heartRateZones" -> userProfile.heartRateZones
                "powerZones" -> userProfile.powerZones
                else -> slopeZones
            }
            val colorResource = getZone(zoneData, value)?.let { if (isPaletteZwift) it.colorZwift else it.colorResource } ?: R.color.zone7
            return ColorProvider(
                day = Color(ContextCompat.getColor(context, colorResource)),
                night = Color(ContextCompat.getColor(context, colorResource))
            )
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

        fun getColorFilter(context: Context, action: KarooAction, colorzone: Boolean): ColorFilter {
            return if (colorzone) {
                ColorFilter.tint(ColorProvider(Color.Black, Color.Black))
            } else {
                ColorFilter.tint(
                    ColorProvider(
                        day = Color(ContextCompat.getColor(context, action.colorday)),
                        night = Color(ContextCompat.getColor(context, action.colornight))
                    )
                )
            }
        }

        fun getFieldSize(size: Int): FieldSize {
            return fieldSizeRanges.first { size in it.min..it.max }.name
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

                            val colorleft = getColorFilter(context, leftAction(settings), leftZone(settings))
                            val colorright = getColorFilter(context, rightAction(settings), rightZone(settings))

                            val colorzoneleft = getColorZone(context, leftAction(settings).zone, leftValue, userProfile, generalsettings.ispalettezwift).takeIf { leftAction(settings).zone == "heartRateZones" || leftAction(settings).zone == "powerZones" || leftAction(settings).zone == "slopeZones"} ?: ColorProvider(Color.White, Color.Black)
                            val colorzoneright= getColorZone(context, rightAction(settings).zone, leftValue, userProfile, generalsettings.ispalettezwift).takeIf { rightAction(settings).zone == "heartRateZones" || rightAction(settings).zone == "powerZones" || rightAction(settings).zone == "slopeZones"} ?: ColorProvider(Color.White, Color.Black)

                            val size = getFieldSize(config.gridSize.second)
                            /*Timber.d("UPDATING leftAction: ${leftAction(settings).action}, rightAction: ${rightAction(settings).action}")
                            Timber.d("Leftvalue is $leftValue and RightIS $rightValue")
                            Timber.d("leftAction.zone is ${leftAction(settings).zone} and rihhAction.zone is ${rightAction(settings).zone}")
                            Timber.d("leftAction.convert is ${leftAction(settings).convert} and rightAction.convert is ${rightAction(settings).convert}")
                            Timber.d("leftAction.colorday is ${leftAction(settings).colorday} and rightAction.colorday is ${rightAction(settings).colorday}")
                            */
                            Timber.d("Viewconfig is $config")

                            val result = glance.compose(context, DpSize.Unspecified) {
                               // DoubleScreenSelector(showh,leftValue, rightValue, leftAction(settings).icon, rightAction(settings).icon, colorleft, colorright, isVertical(settings), colorzoneleft, colorzoneright, config.gridSize.second > 18, karooSystem.hardwareType == HardwareType.KAROO, false, false, generalsettings.iscenteralign)
                                DoubleScreenSelector(showh,leftValue, rightValue, leftAction(settings).icon, rightAction(settings).icon, colorleft, colorright, isVertical(settings), colorzoneleft, colorzoneright, size , karooSystem.hardwareType == HardwareType.KAROO, !(leftAction(settings).convert == "speed" || leftAction(settings).zone=="slopeZones"),!(rightAction(settings).convert == "speed" || rightAction(settings).zone=="slopeZones"),if(showh) generalsettings.iscenteralign else generalsettings.iscentervertical)

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