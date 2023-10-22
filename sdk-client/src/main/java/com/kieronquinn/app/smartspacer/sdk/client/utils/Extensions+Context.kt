package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.AttrRes
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Context.getAttrColor(@AttrRes attr: Int): Int {
    val obtainStyledAttributes: TypedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = obtainStyledAttributes.getColor(0, 0)
    obtainStyledAttributes.recycle()
    return color
}