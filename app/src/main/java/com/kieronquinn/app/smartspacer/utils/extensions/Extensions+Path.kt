package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.Path
import android.graphics.RectF

fun Path.getSize(): RectF {
    return RectF().apply {
        computeBounds(this, true)
    }
}