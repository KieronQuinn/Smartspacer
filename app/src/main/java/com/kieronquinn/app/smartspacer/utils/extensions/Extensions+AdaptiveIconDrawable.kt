package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build

fun AdaptiveIconDrawable.monochromeOrNull(): Drawable? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        monochrome
    } else {
        null
    }
}