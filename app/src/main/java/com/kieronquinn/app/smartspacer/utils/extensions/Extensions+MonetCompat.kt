package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.content.res.ColorStateList
import com.google.android.material.card.MaterialCardView
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