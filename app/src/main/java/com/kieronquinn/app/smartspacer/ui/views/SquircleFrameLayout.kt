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
 * A [FrameLayout] that clips itself and all its children to the Material You "cookie" shape —
 * a 10-lobe scalloped rosette matching the badge used by Google's Glance weather widget.
 *
 * Shape is created via [RoundedPolygon.star] from [androidx.graphics.shapes] (a transitive
 * dependency of [com.google.android.material:material:1.13.0]), using the same parameters as
 * [androidx.compose.material3.MaterialShapes.Cookie9Sided] but with 10 lobes.
 *
 * Set [android:background] for the fill color — the canvas is clipped before the background
 * drawable and all children are drawn, so the background is automatically shaped.
 */
class SquircleFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 9-lobe cookie matching Material3's Cookie9Sided shape from MaterialShapes.
    // innerRadius=0.84f gives the shallow scalloped bumps characteristic of the Material You
    // cookie badge; corner rounding=0.15f smooths peaks and valleys.
    // Polygon is unit-circle normalized (outerRadius=1, centered at 0,0).
    private val polygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
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
