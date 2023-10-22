package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.utils.extensions.dip


class MaxHeightNestedScrollView: NestedScrollView {

    private var maxHeightDp: Float = 0f

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes) {
                setupWithAttrs(attributeSet)
            }

    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    private fun setupWithAttrs(attributeSet: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(
            attributeSet ?: return,
            R.styleable.MaxHeightNestedScrollView,
            0,
            0
        )
        try {
            setMaxHeightDp(
                typedArray.getDimension(R.styleable.MaxHeightNestedScrollView_maxHeightDp, 0f)
            )
        }finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(maxHeightDp != 0f) {
            val maxHeightPx = context.dip(maxHeightDp.toInt())
            val heightSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
            super.onMeasure(widthMeasureSpec, heightSpec)
        }else{
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    fun setMaxHeightDp(maxHeightDp: Float) {
        this.maxHeightDp = maxHeightDp
        invalidate()
    }

}