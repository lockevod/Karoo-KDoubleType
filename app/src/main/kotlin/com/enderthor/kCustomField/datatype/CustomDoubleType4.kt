package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.KarooSystemService

class CustomDoubleType4(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
) : CustomDoubleTypeBase(karooSystem, extension, datatype) {

    override val index = 3
}