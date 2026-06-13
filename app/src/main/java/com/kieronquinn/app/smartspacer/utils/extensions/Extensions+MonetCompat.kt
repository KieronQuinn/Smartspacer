package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb

fun MonetCompat.getColorSurface(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral1[900]!!.toArgb()
    }else{
        getMonetColors().neutral1[10]!!.toArgb()
    }
}

fun MonetCompat.getColorOnSurface(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral1[100]!!.toArgb()
    }else{
        getMonetColors().neutral1[900]!!.toArgb()
    }
}

fun MonetCompat.getColorOnSurfaceVariant(context: Context): Int {
    return if(context.isDarkMode){
        getMonetColors().neutral2[200]!!.toArgb()
    }else{
        getMonetColors().neutral2[700]!!.toArgb()
    }
}

fun MaterialCardView.applyBackgroundTint(monet: MonetCompat) = with(monet) {
    val background = monet.getPrimaryColor(context, !context.isDarkMode)
    backgroundTintList = ColorStateList.valueOf(background)
}

fun MaterialCardView.applyBackgroundSecondary(monet: MonetCompat) = with(monet) {
    val background = monet.getSecondaryColor(context, !context.isDarkMode)
    backgroundTintList = ColorStateList.valueOf(background)
}

fun MonetCompat.getBackgroundForBlur(context: Context, darkMode: Boolean? = null): Int {
    val isDarkMode = darkMode ?: context.isDarkMode
    val light = getMonetColors().accent1[100]?.toArgb()
    val dark = getMonetColors().accent1[800]?.toArgb()
    if (light == null || dark == null) return getBackgroundColor(context)
    val base = (if (isDarkMode) dark else light).withAlpha(0.5f)
    val overlay = if (isDarkMode) light.withAlpha(0.15f) else Color.WHITE.withAlpha(0.32f)
    return ColorUtils.compositeColors(base, overlay)
}

fun MonetCompat.getForegroundForBlur(context: Context): Int {
    val windowBackground = getBackgroundColor(context)
    val primary = getPrimaryColor(context, !context.isDarkMode)
    val highlight = context.getAttrColor(android.R.attr.colorControlHighlight)
    val highlightAlpha = Color.alpha(highlight)
    val tintedHighlight = ColorUtils.setAlphaComponent(primary, highlightAlpha)
    return ColorUtils.compositeColors(tintedHighlight, windowBackground)
}