package com.enderthor.kCustomField.datatype

import com.enderthor.kCustomField.extensions.KarooCustomFieldExtension
import io.hammerhead.karooext.KarooSystemService



class CustomDoubleType(
    karooSystem: KarooSystemService,
    karooExtension: KarooCustomFieldExtension,
    datatype: String,
    value: Int
) : CustomDoubleTypeBase(karooSystem, karooExtension, datatype, value)


