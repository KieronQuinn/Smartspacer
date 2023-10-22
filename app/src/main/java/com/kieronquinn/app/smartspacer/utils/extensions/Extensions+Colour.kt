package com.kieronquinn.app.smartspacer.utils.extensions

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}

fun String.toColorOrNull(): Int? {
    return try {
        Color.parseColor(this)
    }catch (e: IllegalAccessException){
        null
    }
}

fun Int.isColorDark(): Boolean {
    return ColorUtils.calculateLuminance(this) < 0.5
}

fun Int.getContrastColor(): Int {
    return if(isColorDark()) Color.WHITE else Color.BLACK
}

fun Int.withAlpha(alpha: Float): Int {
    return Color.argb((alpha * 255).toInt(), red, green, blue)
}

fun com.google.type.Color.toColour(): Int {
    return if(hasAlpha()){
        Color.valueOf(red, green, blue, alpha.value)
    }else{
        Color.valueOf(red, green, blue)
    }.toArgb()
}