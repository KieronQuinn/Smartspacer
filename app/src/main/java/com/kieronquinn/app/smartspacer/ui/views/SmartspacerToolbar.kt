package com.kieronquinn.app.smartspacer.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.google.android.material.appbar.MaterialToolbar
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.sdk.utils.findViewsByType
import com.kieronquinn.app.smartspacer.utils.extensions.getForegroundForBlur
import com.kieronquinn.app.smartspacer.utils.extensions.isRtl
import com.kieronquinn.monetcompat.core.MonetCompat

class SmartspacerToolbar: MaterialToolbar, ViewGroup.OnHierarchyChangeListener {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?): super(context, attributeSet)
    constructor(context: Context): super(context)

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    private var iconColour = if (isInEditMode) {
        context.getAttrColor(android.R.attr.textColorPrimary)
    } else {
        monet.getForegroundForBlur(context)
    }

    private val padding = resources.getDimensionPixelSize(R.dimen.margin_16)

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        if (child is ViewGroup) {
            child.setOnHierarchyChangeListener(this)
        } else {
            animateActionIconsColourTo(iconColour)
        }
    }

    override fun removeView(view: View?) {
        if (view is ViewGroup) {
            view.setOnHierarchyChangeListener(null)
        }
        super.removeView(view)
    }

    override fun onChildViewAdded(child: View?, parent: View?) {
        animateActionIconsColourTo(iconColour)
    }

    override fun onChildViewRemoved(child: View?, parent: View?) {
        // No-op
    }

    private fun List<View>.getDrawables(): List<GradientDrawable> {
        return map {
            it.setBackgroundResource(R.drawable.toolbar_icon_background)
            val ripple = it.background as RippleDrawable
            val inset = ripple.getDrawable(1) as InsetDrawable
            inset.drawable as GradientDrawable
        }
    }

    fun animateActionIconsColourTo(colour: Int) = post {
        if (!isAttachedToWindow) return@post
        (tag as? ValueAnimator)?.cancel()
        val imageViews = findViewsByType(ImageView::class.java)
        val buttons = findViewsByType(ActionMenuItemView::class.java)
        buttons.forEach {
            it.setPadding(padding)
        }
        imageViews.forEach {
            it.setPadding(padding)
        }
        val drawables = imageViews.getDrawables() + buttons.getDrawables()
        iconColour = colour
        if (drawables.isEmpty()) {
            return@post
        }
        val current = drawables.first().color?.defaultColor ?: Color.TRANSPARENT
        val start = if (current == Color.TRANSPARENT) colour and 0x00FFFFFF else current
        val end = if (colour == Color.TRANSPARENT) current and 0x00FFFFFF else colour
        tag = ValueAnimator.ofArgb(start, end).apply {
            addUpdateListener { anim ->
                drawables.forEach {
                    it.color = ColorStateList.valueOf(anim.animatedValue as Int)
                }
            }
            start()
        }
    }

    fun insetNavigationIcon(padding: Int = resources.getDimensionPixelSize(R.dimen.margin_8)) {
        val navigationIcon = children.filterIsInstance<ImageButton>().firstOrNull() ?: return
        navigationIcon.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            if (isRtl()) {
                updateMargins(right = padding)
            } else {
                updateMargins(left = padding)
            }
        }
    }

}