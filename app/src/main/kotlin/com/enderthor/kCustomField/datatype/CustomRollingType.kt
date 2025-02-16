package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.KarooSystemService



class CustomRollingType(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    value: Int
) : CustomRollingTypeBase(karooSystem, extension, datatype,value)