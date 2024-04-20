package com.kieronquinn.app.smartspacer.utils.viewpager

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kieronquinn.app.smartspacer.utils.extensions.onPageScrolled

object ViewPager2ViewHeightAnimator {

    suspend fun register(viewPager2: ViewPager2) {
        viewPager2.onPageScrolled().collect {
            val position = it.first
            val positionOffset = it.second
            viewPager2.recalculate(position, positionOffset)
        }
    }

    private val ViewPager2.layoutManager: LinearLayoutManager? get() =
        (getChildAt(0) as? RecyclerView)?.layoutManager as? LinearLayoutManager

    private fun ViewPager2.recalculate(
        position: Int,
        positionOffset: Float = 0f
    ) = layoutManager?.apply {
        val leftView = findViewByPosition(position) ?: return@apply
        val rightView = findViewByPosition(position + 1)
        this@recalculate.apply {
            val leftHeight = getMeasuredViewHeightFor(leftView)
            layoutParams = layoutParams.apply {
                height = if (rightView != null) {
                    val rightHeight = getMeasuredViewHeightFor(rightView)
                    leftHeight + ((rightHeight - leftHeight) * positionOffset).toInt()
                } else {
                    leftHeight
                }
            }
            invalidate()
        }
    }

    private fun getMeasuredViewHeightFor(view: View) : Int {
        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(wMeasureSpec, hMeasureSpec)
        return view.measuredHeight
    }

}