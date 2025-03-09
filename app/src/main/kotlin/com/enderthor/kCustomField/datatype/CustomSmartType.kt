package com.enderthor.kCustomField.datatype

import io.hammerhead.karooext.KarooSystemService



class CustomSmartType(
    karooSystem: KarooSystemService,

    datatype: String,
    value: Int
) : CustomSmartTypeBase(karooSystem,  datatype,value)