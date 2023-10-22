package com.kieronquinn.app.smartspacer.utils.extensions

import android.util.DisplayMetrics
import kotlin.math.max
import kotlin.math.min

val DisplayMetrics.portraitWidthPixels: Int
    get() = min(widthPixels, heightPixels)

val DisplayMetrics.portraitHeightPixels: Int
    get() = max(widthPixels, heightPixels)