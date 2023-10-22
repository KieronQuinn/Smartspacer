package com.kieronquinn.app.smartspacer.utils

import android.content.pm.ParceledListSlice
import android.os.Parcelable

fun <T : Parcelable?> mockParceledListSlice(
    data: List<T> = emptyList()
) = ParceledListSlice(data)