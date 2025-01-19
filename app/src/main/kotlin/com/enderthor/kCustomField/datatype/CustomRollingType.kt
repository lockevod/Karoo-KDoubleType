package com.enderthor.kCustomField.datatype

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class CustomRollingType(
    karooSystem: KarooSystemService,
    extension: String,
    datatype: String,
    context: Context,
    value: Int
) : CustomRollingTypeBase(karooSystem, extension, datatype, context,value)