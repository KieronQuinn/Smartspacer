package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * A [FrameLayout] that clips itself and all its children to a 10-lobe Material You cookie shape.
 *
 * Material3's MaterialShapes only ships Cookie9Sided and Cookie12Sided; there is no Cookie10Sided.
 * We construct the 10-lobe variant directly via [RoundedPolygon.star] from
 * [androidx.graphics.shapes], which is a transitive dependency of material:1.13.0+.
 *
 * Set [android:background] for the fill color — the canvas is clipped before the background
 * drawable and all children are drawn, so the background is automatically shaped.
 */
class SquircleFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 10-lobe cookie — same aesthetic as Material3's Cookie9Sided/Cookie12Sided but with 10 bumps.
    // innerRadius=0.84f gives shallow scalloped bumps; corner rounding=0.15f smooths peaks/valleys.
    // Polygon is unit-circle normalized (outerRadius=1, centered at 0,0).
    private val polygon = RoundedPolygon.star(
        numVerticesPerRadius = 10,
        innerRadius = 0.84f,
        rounding = CornerRounding(radius = 0.15f)
    )

    private val scaledPath = Path()
    private val matrix = Matrix()

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        // Polygon coords are in [-1, +1]; scale to fill the view and center it.
        matrix.setScale(w / 2f, h / 2f)
        matrix.postTranslate(w / 2f, h / 2f)
        scaledPath.set(polygon.toPath())
        scaledPath.transform(matrix)
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(scaledPath)
        super.draw(canvas)
        canvas.restore()
    }
}
