package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.cos
import kotlin.math.min

/**
 * A [FrameLayout] that clips itself and all children to a "cookie" / scalloped-rosette shape —
 * the same style used by the Google Glance widget's weather badge.
 *
 * The shape is defined by the polar equation:
 *   r(θ) = baseRadius + amplitude · cos(lobes · θ)
 *
 * With [lobes] = 10 this produces the 10-lobe cookie used by Google's Glance weather badge.
 * Set [android:background] to fill the cookie shape; everything (background + children) is
 * clipped to the path before drawing.
 */
class SquircleFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Number of lobes. Google Glance uses 10. */
    private val lobes = 10

    /** Fraction of the half-dimension used as the base (centre) radius. */
    private val baseFraction = 0.82f

    /** Fraction of the half-dimension used as the lobe amplitude. */
    private val ampFraction = 0.18f

    /** Sample count — 720 gives sub-0.5° steps, which is visually smooth on any display. */
    private val steps = 720

    private val cookiePath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        buildPath(w.toFloat(), h.toFloat())
    }

    private fun buildPath(w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val halfMin = min(cx, cy)
        val baseR = halfMin * baseFraction
        val amp   = halfMin * ampFraction

        cookiePath.reset()
        for (i in 0..steps) {
            val theta = (2.0 * Math.PI * i / steps)
            val r = baseR + amp * cos((lobes * theta).toFloat())
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) cookiePath.moveTo(x, y) else cookiePath.lineTo(x, y)
        }
        cookiePath.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(cookiePath)
        super.draw(canvas)
        canvas.restore()
    }
}
