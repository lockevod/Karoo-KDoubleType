package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.KarooSystemService

class CustomDoubleType3(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
) : CustomDoubleTypeBase(karooSystem, extension, datatype) {
    override val index = 2
}