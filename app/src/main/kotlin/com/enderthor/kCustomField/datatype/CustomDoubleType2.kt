package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomDoubleType2(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomDoubleTypeBase(karooSystem, extension, datatype, context) {
    override val leftAction = { settings: CustomFieldSettings -> settings.customleft2 }
    override val rightAction = { settings: CustomFieldSettings -> settings.customright2 }
    override val isVertical = { settings: CustomFieldSettings -> settings.isvertical2 }
    override val leftZone = { settings: CustomFieldSettings -> settings.customleft2zone }
    override val rightZone = { settings: CustomFieldSettings -> settings.customright2zone }
    override val showh = true
}