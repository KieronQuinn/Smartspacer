package com.kieronquinn.app.smartspacer.ui.views

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint

/**
 * Drawable that renders a single Material Symbols Outlined glyph using [typeface].
 *
 * Usage:
 *   button.icon = MaterialSymbolDrawable(codepoint, typeface, 20.dp, Color.WHITE)
 */
class MaterialSymbolDrawable(
    private val codepoint: Int,
    private val typeface: Typeface,
    private val sizePx: Int,
    tintColor: Int
) : Drawable() {

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = sizePx * 0.85f
        color = tintColor
    }

    private val char = String(Character.toChars(codepoint))

    override fun draw(canvas: Canvas) {
        val b = bounds
        val x = b.exactCenterX() - paint.measureText(char) / 2f
        val fm = paint.fontMetrics
        val y = b.exactCenterY() - (fm.descent + fm.ascent) / 2f
        canvas.drawText(char, x, y, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx
}
