package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomDoubleVerticalType2(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomDoubleTypeBase(karooSystem, extension, datatype,context) {
    override val leftAction = { settings: CustomFieldSettings -> settings.customverticalleft2 }
    override val rightAction = { settings: CustomFieldSettings -> settings.customverticalright2 }
    override val isVertical = { settings: CustomFieldSettings -> settings.ishorizontal2 }
    override val leftZone = { settings: CustomFieldSettings -> settings.customverticalleft2zone }
    override val rightZone = { settings: CustomFieldSettings -> settings.customverticalright2zone }
    override val showh = false
}