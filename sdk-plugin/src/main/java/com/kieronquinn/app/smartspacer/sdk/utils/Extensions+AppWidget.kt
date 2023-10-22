package com.kieronquinn.app.smartspacer.sdk.utils

import android.content.res.Resources

private val resources = Resources.getSystem()

fun getColumnSpan(spanX: Int): Int {
    return (spanX * resources.dip(74)) - resources.dip(2)
}

fun getRowSpan(spanY: Int): Int {
    return (spanY * resources.dip(74)) - resources.dip(2)
}

private fun Resources.dip(value: Int): Int = (value * displayMetrics.density).toInt()