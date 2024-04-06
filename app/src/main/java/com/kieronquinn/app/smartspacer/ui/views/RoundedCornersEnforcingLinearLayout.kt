package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.core.view.setPadding
import com.kieronquinn.app.smartspacer.utils.appwidget.RoundedCornerEnforcement

open class RoundedCornersEnforcingLinearLayout: LinearLayout {

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr)

    private val mEnforcedRectangle: Rect = Rect()
    private var mEnforcedCornerRadius = 0f

    private val mCornerRadiusEnforcementOutline: ViewOutlineProvider =
        object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline) {
                if (mEnforcedRectangle.isEmpty || mEnforcedCornerRadius <= 0) {
                    outline.setEmpty()
                } else {
                    outline.setRoundRect(mEnforcedRectangle, mEnforcedCornerRadius)
                }
            }
        }

    override fun addView(child: View?) {
        super.addView(child)
        setPadding(0)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        enforceRoundedCorners()
    }

    private fun resetRoundedCorners() {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = false
    }

    private fun enforceRoundedCorners() {
        if (mEnforcedCornerRadius <= 0) {
            resetRoundedCorners()
            return
        }
        val background: View? = RoundedCornerEnforcement.findBackground(this)
        if (background == null || RoundedCornerEnforcement.hasAppWidgetOptedOut(this, background)) {
            resetRoundedCorners()
            return
        }
        RoundedCornerEnforcement.computeRoundedRectangle(
            this,
            background,
            mEnforcedRectangle
        )
        outlineProvider = mCornerRadiusEnforcementOutline
        clipToOutline = true
    }

    init {
        mEnforcedCornerRadius = RoundedCornerEnforcement.computeEnforcedRadius(getContext())
    }

}