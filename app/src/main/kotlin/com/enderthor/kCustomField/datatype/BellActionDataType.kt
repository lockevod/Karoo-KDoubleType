package com.enderthor.kCustomField.datatype

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.RemoteViewsCompositionResult
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.enderthor.kCustomField.R
import timber.log.Timber


@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class BellActionDataType(
    datatype: String,
) : DataTypeImpl("kcustomfield", datatype) {

    private val glance = GlanceRemoteViews()


    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scopeJob = Job()
        val scope = CoroutineScope(Dispatchers.IO + scopeJob)

        val configJob = scope.launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            emitter.onNext(ShowCustomStreamState(message = "", color = null))
            awaitCancellation()
        }


        val viewJob = scope.launch {
            try {

                val result = updateConfigDataView(
                    context = context,
                    config = config,
                )
                emitter.updateView(result.remoteViews)

            } catch (e: CancellationException) {
                //Timber.d(e, "Cancelación normal del flujo")
            } catch (e: Exception) {
                Timber.e(e, "Error actualizando vista: ${e.message}")

                val result = updateConfigDataView(
                    context = context,
                    config = config,
                )
                emitter.updateView(result.remoteViews)
                delay(5000)
            }

        }

        emitter.setCancellable {
            configJob.cancel()
            viewJob.cancel()
            scope.cancel()
            scopeJob.cancel()
        }
    }


    private suspend fun updateConfigDataView(
        context: Context,
        config: ViewConfig,
    ): RemoteViewsCompositionResult {


        return glance.compose(context, DpSize.Unspecified) {
            var modifier = GlanceModifier.fillMaxSize().padding(5.dp)

            if (!config.preview) {

                modifier = modifier.clickable(
                    onClick = actionRunCallback<BellAction>(
                    )
                )
            }

            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {

                Image(
                    provider = ImageProvider(R.drawable.ic_bell),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                   // modifier = GlanceModifier.size(iconSize).padding(top = topPadding),
                    colorFilter = ColorFilter.tint( ColorProvider(Color.Black, Color.White))
                )
            }
        }
    }
}