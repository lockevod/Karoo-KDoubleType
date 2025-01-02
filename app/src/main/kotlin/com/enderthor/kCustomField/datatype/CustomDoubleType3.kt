package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomDoubleType3(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomDoubleTypeBase(karooSystem, extension, datatype, context) {
    override val leftAction = { settings: CustomFieldSettings -> settings.customleft3}
    override val rightAction = { settings: CustomFieldSettings -> settings.customright3}
    override val isVertical = { settings: CustomFieldSettings -> settings.isvertical3}
    override val leftZone = { settings: CustomFieldSettings -> settings.customleft3zone }
    override val rightZone = { settings: CustomFieldSettings -> settings.customright3zone }
    override val showh = true
}