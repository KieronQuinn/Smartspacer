package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A [FrameLayout] that clips itself and all its children to the 10-sided cookie shape
 * from the Figma design.
 *
 * The path is taken verbatim from the SVG exported by the designer (shift +7x, +4y so all
 * coordinates are positive; original range was roughly −6..406 × −3..404).
 * [onSizeChanged] scales the base path from its natural 413 × 409 coordinate space to the
 * actual view size, so the shape fills the view regardless of display density.
 *
 * Set [android:background] for the fill color — the canvas is clipped before the background
 * drawable and all children are drawn, so the fill is automatically shaped.
 */
class SquircleFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Figma cookie path in SVG coordinate space.
    // All coordinates shifted by (+7, +4) to make every value ≥ 0.
    // Resulting bounds: x 0.94..412.75, y 0.61..408.18 → viewport 413 × 409.
    private val basePath = Path().also { p ->
        val dx = 7f; val dy = 4f
        p.moveTo(181.649f + dx, 3.14f + dy)
        p.cubicTo(177.117f + dx, 4.758f + dy, 168.239f + dx, 9.878f + dy, 161.92f + dx, 14.517f + dy)
        p.cubicTo(146.831f + dx, 25.593f + dy, 132.708f + dx, 29.827f + dy, 116.807f + dx, 28.039f + dy)
        p.cubicTo(89.405f + dx, 24.959f + dy, 65.738f + dx, 41.711f + dy, 56.323f + dx, 70.853f + dy)
        p.cubicTo(50.002f + dx, 90.42f + dy, 44.932f + dx, 97.488f + dy, 30.581f + dx, 106.74f + dy)
        p.cubicTo(0.831f + dx, 125.92f + dy, -6.057f + dx, 145.732f + dy, 5.58f + dx, 178.652f + dy)
        p.cubicTo(12.49f + dx, 198.198f + dy, 12.49f + dx, 201.802f + dy, 5.58f + dx, 221.348f + dy)
        p.cubicTo(-5.932f + dx, 253.914f + dy, 1.187f + dx, 274.514f + dy, 30.412f + dx, 293.21f + dy)
        p.cubicTo(44.906f + dx, 302.481f + dy, 49.979f + dx, 309.509f + dy, 56.298f + dx, 329.069f + dy)
        p.cubicTo(65.942f + dx, 358.92f + dy, 83.328f + dx, 371.017f + dy, 117.845f + dx, 371.894f + dy)
        p.cubicTo(141.321f + dx, 372.49f + dy, 144.858f + dx, 373.653f + dy, 164.861f + dx, 387.348f + dy)
        p.cubicTo(188.219f + dx, 403.339f + dy, 210.764f + dx, 404.181f + dy, 230.837f + dx, 389.81f + dy)
        p.cubicTo(250.995f + dx, 375.38f + dy, 259.864f + dx, 372.267f + dy, 282.155f + dx, 371.802f + dy)
        p.cubicTo(316.609f + dx, 371.083f + dy, 334.698f + dx, 358.538f + dy, 343.477f + dx, 329.275f + dy)
        p.cubicTo(349.022f + dx, 310.794f + dy, 356.253f + dx, 301.125f + dy, 371.382f + dx, 291.966f + dy)
        p.cubicTo(398.529f + dx, 275.531f + dy, 405.749f + dx, 253.398f + dy, 394.42f + dx, 221.348f + dy)
        p.cubicTo(387.51f + dx, 201.802f + dy, 387.51f + dx, 198.198f + dy, 394.42f + dx, 178.652f + dy)
        p.cubicTo(405.846f + dx, 146.328f + dy, 398.578f + dx, 125.335f + dy, 369.525f + dx, 106.75f + dy)
        p.cubicTo(355.45f + dx, 97.747f + dy, 347.937f + dx, 87.114f + dy, 342.799f + dx, 68.928f + dy)
        p.cubicTo(335.379f + dx, 42.662f + dy, 309.61f + dx, 24.842f + dy, 283.713f + dx, 28.067f + dy)
        p.cubicTo(267.903f + dx, 30.036f + dy, 251.505f + dx, 25.034f + dy, 236.382f + dx, 13.63f + dy)
        p.cubicTo(218.53f + dx, 0.167f + dy, 199.945f + dx, -3.395f + dy, 181.649f + dx, 3.14f + dy)
        p.close()
    }

    private val scaledPath = Path()
    private val matrix = Matrix()

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        // Scale from the 413 × 409 SVG space to the actual view dimensions.
        matrix.setScale(w / 413f, h / 409f)
        scaledPath.set(basePath)
        scaledPath.transform(matrix)
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(scaledPath)
        super.draw(canvas)
        canvas.restore()
    }
}
