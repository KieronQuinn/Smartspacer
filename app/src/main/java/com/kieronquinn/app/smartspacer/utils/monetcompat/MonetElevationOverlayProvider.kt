/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kieronquinn.app.smartspacer.utils.monetcompat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.google.android.material.internal.ViewUtils
import com.kieronquinn.app.smartspacer.utils.extensions.getColorOnSurface
import com.kieronquinn.app.smartspacer.utils.extensions.getColorOnSurfaceVariant
import com.kieronquinn.app.smartspacer.utils.extensions.getColorSurface
import com.kieronquinn.monetcompat.core.MonetCompat

@SuppressLint("RestrictedApi")
/** Utility for calculating elevation overlay alpha values and colors.  */
class MonetElevationOverlayProvider private constructor(
    /** Returns the current theme's boolean value for `R.attr.elevationOverlayEnabled`.  */
    private val isThemeElevationOverlayEnabled: Boolean,
    /** Returns the current theme's color int value for `R.attr.elevationOverlayColor`.  */
    @get:ColorInt
    @param:ColorInt val themeElevationOverlayColor: Int,
    @param:ColorInt private val elevationOverlayAccentColor: Int,
    /** Returns the current theme's color int value for `R.attr.colorSurface`.  */
    @get:ColorInt
    @param:ColorInt val themeSurfaceColor: Int,
    private val displayDensity: Float
) {

    constructor(context: Context, monet: MonetCompat = MonetCompat.getInstance()) : this(
        true,
        monet.getColorOnSurface(context),
        monet.getColorOnSurfaceVariant(context),
        monet.getColorSurface(context),
        context.resources.displayMetrics.density
    )

    /**
     * See [.compositeOverlayWithThemeSurfaceColorIfNeeded].
     *
     *
     * The absolute elevation of the parent of the provided `overlayView` will also be
     * factored in when determining the overlay color.
     */
    @ColorInt
    fun compositeOverlayWithThemeSurfaceColorIfNeeded(
        elevation: Float, overlayView: View
    ): Int {
        var elevation = elevation
        elevation += getParentAbsoluteElevation(overlayView)
        return compositeOverlayWithThemeSurfaceColorIfNeeded(elevation)
    }

    /**
     * Blends the calculated elevation overlay color (@see #compositeOverlayIfNeeded(int, float)) with
     * the current theme's color int value for `R.attr.colorSurface` if needed.
     */
    @ColorInt
    fun compositeOverlayWithThemeSurfaceColorIfNeeded(elevation: Float): Int {
        return compositeOverlayIfNeeded(themeSurfaceColor, elevation)
    }

    /**
     * See [.compositeOverlayIfNeeded].
     *
     *
     * The absolute elevation of the parent of the provided `overlayView` will also be
     * factored in when determining the overlay color.
     */
    @ColorInt
    fun compositeOverlayIfNeeded(
        @ColorInt backgroundColor: Int, elevation: Float, overlayView: View
    ): Int {
        var elevation = elevation
        elevation += getParentAbsoluteElevation(overlayView)
        return compositeOverlayIfNeeded(backgroundColor, elevation)
    }

    /**
     * Blends the calculated elevation overlay color (@see #compositeOverlay(int, float)) with the
     * `backgroundColor`, only if the current theme's `R.attr.elevationOverlayEnabled` is
     * true and the `backgroundColor` matches the theme's surface color (`R.attr.colorSurface`); otherwise returns the `backgroundColor`.
     */
    @ColorInt
    fun compositeOverlayIfNeeded(@ColorInt backgroundColor: Int, elevation: Float): Int {
        return if (isThemeElevationOverlayEnabled && isThemeSurfaceColor(backgroundColor)) {
            compositeOverlay(backgroundColor, elevation)
        } else {
            backgroundColor
        }
    }

    /** See [.compositeOverlay].  */
    @ColorInt
    fun compositeOverlay(
        @ColorInt backgroundColor: Int, elevation: Float, overlayView: View
    ): Int {
        var elevation = elevation
        elevation += getParentAbsoluteElevation(overlayView)
        return compositeOverlay(backgroundColor, elevation)
    }

    /**
     * Blends the calculated elevation overlay color with the provided `backgroundColor`.
     *
     *
     * An alpha level is applied to the theme's `R.attr.elevationOverlayColor` by using a
     * formula that is based on the provided `elevation` value.
     */
    @ColorInt
    fun compositeOverlay(@ColorInt backgroundColor: Int, elevation: Float): Int {
        val overlayAlphaFraction = calculateOverlayAlphaFraction(elevation)
        val backgroundAlpha = Color.alpha(backgroundColor)
        val backgroundColorOpaque = ColorUtils.setAlphaComponent(backgroundColor, 255)
        var overlayColorOpaque = MaterialColors.layer(
            backgroundColorOpaque,
            themeElevationOverlayColor,
            overlayAlphaFraction
        )
        if (overlayAlphaFraction > 0 && elevationOverlayAccentColor != Color.TRANSPARENT) {
            val overlayAccentColor = ColorUtils.setAlphaComponent(
                elevationOverlayAccentColor, OVERLAY_ACCENT_COLOR_ALPHA
            )
            overlayColorOpaque = MaterialColors.layer(overlayColorOpaque, overlayAccentColor)
        }
        return ColorUtils.setAlphaComponent(overlayColorOpaque, backgroundAlpha)
    }

    /**
     * Calculates the alpha value, between 0 and 255, that should be used with the elevation overlay
     * color, based on the provided `elevation` value.
     */
    fun calculateOverlayAlpha(elevation: Float): Int {
        return Math.round(calculateOverlayAlphaFraction(elevation) * 255)
    }

    /**
     * Calculates the alpha fraction, between 0 and 1, that should be used with the elevation overlay
     * color, based on the provided `elevation` value.
     */
    fun calculateOverlayAlphaFraction(elevation: Float): Float {
        if (displayDensity <= 0 || elevation <= 0) {
            return 0f
        }
        val elevationDp = elevation / displayDensity
        val alphaFraction = (FORMULA_MULTIPLIER * Math.log1p(elevationDp.toDouble())
            .toFloat() + FORMULA_OFFSET) / 100
        return Math.min(alphaFraction, 1f)
    }

    /**
     * Returns the absolute elevation of the parent of the provided `overlayView`, or in other
     * words, the sum of the elevations of all ancestors of the `overlayView`.
     */
    fun getParentAbsoluteElevation(overlayView: View): Float {
        return ViewUtils.getParentAbsoluteElevation(overlayView)
    }

    private fun isThemeSurfaceColor(@ColorInt color: Int): Boolean {
        return ColorUtils.setAlphaComponent(color, 255) == themeSurfaceColor
    }

    companion object {
        private const val FORMULA_MULTIPLIER = 4.5f
        private const val FORMULA_OFFSET = 2f
        private val OVERLAY_ACCENT_COLOR_ALPHA = Math.round(0.02 * 255).toInt()
    }
}