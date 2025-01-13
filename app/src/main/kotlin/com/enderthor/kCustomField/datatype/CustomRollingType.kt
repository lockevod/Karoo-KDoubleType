package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.KarooSystemService

class CustomRollingType(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    value: Int
) : CustomDoubleTypeBase(karooSystem, extension, datatype, value)