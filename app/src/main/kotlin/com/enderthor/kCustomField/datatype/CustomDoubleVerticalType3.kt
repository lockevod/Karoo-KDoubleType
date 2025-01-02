package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomDoubleVerticalType3(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomDoubleTypeBase(karooSystem, extension, datatype, context) {
    override val leftAction = { settings: CustomFieldSettings -> settings.customverticalleft3 }
    override val rightAction = { settings: CustomFieldSettings -> settings.customverticalright3 }
    override val isVertical = { settings: CustomFieldSettings -> settings.ishorizontal3 }
    override val leftZone = { settings: CustomFieldSettings -> settings.customverticalleft1zone }
    override val rightZone = { settings: CustomFieldSettings -> settings.customverticalright1zone }
    override val showh = false
}