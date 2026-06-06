package com.kieronquinn.app.smartspacer.ui.drawables

import android.graphics.drawable.DrawableWrapper
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import com.android.internal.graphics.drawable.BackgroundBlurDrawable

/**
 *  Compatibility wrapper for [BackgroundBlurDrawable]
 */
@RequiresApi(Build.VERSION_CODES.S)
class SmartspacerBackgroundBlurDrawable(
    private val drawable: BackgroundBlurDrawable
): DrawableWrapper(drawable) {

    /**
     * Color that will be alpha blended on top of the blur.
     */
    fun setColor(@ColorInt color: Int) {
        drawable.setColor(color)
    }

    /**
     * Blur radius in pixels.
     */
    fun setBlurRadius(blurRadius: Int) {
        drawable.setBlurRadius(blurRadius)
    }

    /**
     * Sets the corner radius, in degrees.
     */
    fun setCornerRadius(cornerRadius: Float) {
        drawable.setCornerRadius(cornerRadius)
    }

    fun setCornerRadius(
        cornerRadiusTL: Float,
        cornerRadiusTR: Float,
        cornerRadiusBL: Float,
        cornerRadiusBR: Float
    ) {
        drawable.setCornerRadius(cornerRadiusTL, cornerRadiusTR, cornerRadiusBL, cornerRadiusBR)
    }

}