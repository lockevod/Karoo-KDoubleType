package com.enderthor.kCustomField.datatype

import android.content.Context

import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.enderthor.kCustomField.extensions.KarooCustomFieldExtension
import com.enderthor.kCustomField.extensions.streamGeneralSettings

import kotlinx.coroutines.flow.first


class BellAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val karooSystem = KarooCustomFieldExtension.instance.karooSystem
        val currentBell = context.streamGeneralSettings().first().bellBeepKey
        karooSystem.dispatch(currentBell.action)
    }
}