package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomDoubleType1(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomDoubleTypeBase(karooSystem, extension, datatype, context) {
    override val leftAction = { settings: CustomFieldSettings -> settings.customleft1 }
    override val rightAction = { settings: CustomFieldSettings -> settings.customright1 }
    override val isVertical = { settings: CustomFieldSettings -> settings.isvertical1 }
    override val leftZone = { settings: CustomFieldSettings -> settings.customleft1zone }
    override val rightZone = { settings: CustomFieldSettings -> settings.customright1zone }
    override val showh = true
}