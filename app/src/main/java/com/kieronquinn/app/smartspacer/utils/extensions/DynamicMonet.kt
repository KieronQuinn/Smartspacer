package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor

/**
 * Drop-in replacement for MonetCompat backed by Android 12 DynamicColors.
 * All color reads go through Material 3 theme attributes, which DynamicColors
 * (already applied in Smartspacer.Application.onCreate) patches with wallpaper-derived
 * Monet colors on API 31+.
 */
class DynamicMonet private constructor() {

    companion object {
        private val INSTANCE = DynamicMonet()

        /** Drop-in for MonetCompat.getInstance(). */
        @JvmStatic
        fun getInstance(): DynamicMonet = INSTANCE
    }

    fun getBackgroundColor(ctx: Context): Int =
        ctx.getAttrColor(android.R.attr.colorBackground)

    fun getBackgroundColorSecondary(ctx: Context): Int? =
        ctx.getAttrColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)

    fun getAccentColor(ctx: Context): Int =
        ctx.getAttrColor(androidx.appcompat.R.attr.colorPrimary)

    fun getAccentColor(ctx: Context, isDark: Boolean?): Int =
        ctx.getAttrColor(androidx.appcompat.R.attr.colorPrimary)

    fun getPrimaryColor(ctx: Context): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorSecondaryContainer)

    fun getPrimaryColor(ctx: Context, isLight: Boolean?): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorSecondaryContainer)

    fun getSecondaryColor(ctx: Context, isLight: Boolean?): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorTertiaryContainer)

    fun getColorSurface(ctx: Context): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorSurface)

    fun getColorOnSurface(ctx: Context): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurface)

    fun getColorOnSurfaceVariant(ctx: Context): Int =
        ctx.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

    /**
     * No-op — DynamicColors colors are available immediately, no async initialization needed.
     * Replaces MonetCompat.awaitMonetReady().
     */
    suspend fun awaitMonetReady() {}

    /** No-op — DynamicColors handles updates via activity recreation on wallpaper change. */
    fun addMonetColorsChangedListener(listener: Any, immediate: Boolean = false) {}

    /** No-op. */
    fun removeMonetColorsChangedListener(listener: Any) {}

    /**
     * Wallpaper color selection is handled by the system via DynamicColors on API 31+.
     * Returns null to indicate no manual color selection is available.
     */
    fun getAvailableWallpaperColors(): List<Int>? = null

    /** Returns null — wallpaper color selection is managed by the system. */
    fun getSelectedWallpaperColor(): Int? = null

    /** No-op — DynamicColors picks up wallpaper changes automatically. */
    fun updateMonetColors() {}
}

// ── Extension functions to match MonetCompat extension API ────────────────────

fun DynamicMonet.getColorSurface(context: Context): Int =
    context.getAttrColor(com.google.android.material.R.attr.colorSurface)

fun DynamicMonet.getColorOnSurface(context: Context): Int =
    context.getAttrColor(com.google.android.material.R.attr.colorOnSurface)

fun DynamicMonet.getColorOnSurfaceVariant(context: Context): Int =
    context.getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

fun MaterialCardView.applyBackgroundTint(monet: DynamicMonet) {
    backgroundTintList = ColorStateList.valueOf(
        context.getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer)
    )
}

fun MaterialCardView.applyBackgroundSecondary(monet: DynamicMonet) {
    backgroundTintList = ColorStateList.valueOf(
        context.getAttrColor(com.google.android.material.R.attr.colorSecondaryContainer)
    )
}

/** Replaces MonetCompat's overrideRippleColor extension. */
fun View.overrideRippleColor(color: Int? = null, colorStateList: ColorStateList? = null) {
    val ripple = (background as? RippleDrawable)
        ?: (foreground as? RippleDrawable)
        ?: return
    when {
        colorStateList != null -> ripple.setColor(colorStateList)
        color != null -> ripple.setColor(ColorStateList.valueOf(color))
    }
}

/** Replaces MonetCompat's applyMonet() on MaterialSwitch — MaterialSwitch is self-theming via M3. */
fun MaterialSwitch.applyMonet() {
    // MaterialSwitch themes itself from ?attr/colorPrimary etc. via DynamicColors — no-op needed.
}

/**
 * Replaces MonetCompat's generic applyMonet() extension on any View.
 * All Material 3 views (MaterialButton, TextInputLayout, Slider, ProgressIndicator, etc.)
 * self-theme from ?attr/colorPrimary etc. via DynamicColors — no manual tinting needed.
 */
fun View.applyMonet() {
    // No-op: M3 components auto-theme via DynamicColors.
}

/** Snackbar self-themes via M3/DynamicColors. */
fun Snackbar.applyMonet() {
    // No-op.
}
