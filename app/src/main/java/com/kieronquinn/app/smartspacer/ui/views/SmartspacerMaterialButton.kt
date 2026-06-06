package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.kieronquinn.app.smartspacer.ui.drawables.SmartspacerBackgroundBlurDrawable

/**
 *  [MaterialButton] that supports using a [SmartspacerBackgroundBlurDrawable] for the background,
 *  by removing the regular background and drawing the blur on top, while maintaining the corner
 *  radii. If the background is not a blur, this will behave as a regular [MaterialButton].
 */
class SmartspacerMaterialButton: MaterialButton {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    private var shapeDrawable = findShapeDrawable()
    private var blur: SmartspacerBackgroundBlurDrawable? = null

    override fun setBackground(background: Drawable) {
        when (background) {
            is SmartspacerBackgroundBlurDrawable -> {
                blur = background
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                invalidate()
            }
            is ColorDrawable -> {
                blur = null
                backgroundTintList = ColorStateList.valueOf(background.color)
                invalidate()
            }
            else -> super.setBackgroundDrawable(background).also {
                shapeDrawable = findShapeDrawable()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        val shapeDrawable = shapeDrawable
        val blur = blur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur != null && shapeDrawable != null) {
            blur.setBounds(0, 0, width, height)
            val topLeft = shapeDrawable.topLeftCornerResolvedSize
            val topRight = shapeDrawable.topRightCornerResolvedSize
            val bottomLeft = shapeDrawable.bottomLeftCornerResolvedSize
            val bottomRight = shapeDrawable.bottomRightCornerResolvedSize
            blur.setCornerRadius(
                topLeft,
                topRight,
                bottomLeft,
                bottomRight
            )
            blur.draw(canvas)
        }
        super.draw(canvas)
    }

    private fun findShapeDrawable(): MaterialShapeDrawable? {
        val ripple = when (val background = background) {
            is RippleDrawable -> background
            is MaterialShapeDrawable -> return background
            else -> return null
        }
        val drawables = ArrayList<Drawable>()
        for (i in 0 until ripple.numberOfLayers) {
            drawables.add(ripple.getDrawable(i))
        }
        return drawables.filterIsInstance<MaterialShapeDrawable>().firstOrNull()
    }

}