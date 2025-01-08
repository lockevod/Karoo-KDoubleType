package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomRollingType2(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context
) : CustomRollingTypeBase(karooSystem, extension, datatype, context) {

    override val firstField = { settings: OneFieldSettings -> settings.onefield }
    override val secondField = { settings: OneFieldSettings -> settings.secondfield }
    override val thirdField = { settings: OneFieldSettings -> settings.thirdfield }
    override val time = { settings: OneFieldSettings -> settings.rollingtime }
    override val index = 1
}