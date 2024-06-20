package com.kieronquinn.app.smartspacer.ui.views.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ListViewHidden
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.recyclerview.widget.RecyclerView
import com.kieronquinn.app.smartspacer.R

/**
 *  A [ListView] with [NestedScrollingChild] support, for seamless vertical scrolling, as well
 *  as automatic passing of item long click events onto a parent with the ID of
 *  [R.id.item_expanded_widget_container].
 */
class WidgetListView constructor(context: Context, attributeSet: AttributeSet?) :
    ListViewHidden(context, attributeSet, 0, 0), NestedScrollingChild, AbsListView.OnScrollListener {

    private val mScrollingChildHelper = NestedScrollingChildHelper(this)
    private var externalOnScrollListener: OnScrollListener? = null

    private val scrollingParent by lazy {
        var scrollingView: RecyclerView? = null
        var parentView = parent
        while(parentView.parent != null){
            if(parentView is RecyclerView){
                scrollingView = parentView as RecyclerView
            }
            parentView = parentView.parent
        }
        scrollingView
    }

    private var isAtTop = true
    private var isAtBottom = false

    init {
        isNestedScrollingEnabled = true
        super.setOnScrollListener(this)
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return mScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        if(isAtTop && dy < 0){
            scrollingParent?.scrollBy(dx, dy)
            return true
        } else if(isAtBottom && dy > 0){
            scrollingParent?.scrollBy(dx, dy)
            return true
        }
        return mScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        if(isAtTop && velocityY < 0){
            scrollingParent?.fling((velocityX / 3f).toInt(), (velocityY / 3f).toInt())
            return true
        } else if(isAtBottom && velocityY > 0){
            scrollingParent?.fling((velocityX / 3f).toInt(), (velocityY / 3f).toInt())
            return true
        }
        return mScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onScrollStateChanged(view: AbsListView, state: Int) {
        externalOnScrollListener?.onScrollStateChanged(view, state)
    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        isAtTop = computeVerticalScrollOffset() == 0
        isAtBottom = firstVisibleItem + visibleItemCount >= totalItemCount
        externalOnScrollListener?.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLongClickListenerFromParent()
    }

    private fun setLongClickListenerFromParent() {
        val parent = findContainerParent() ?: return
        setOnItemLongClickListener { _, _, _, _ ->
            parent.performLongClick(0f, 0f)
        }
    }

    private fun View.findContainerParent(): View? {
        if(this.id == R.id.item_expanded_widget_container) return this
        return (parent as? View)?.findContainerParent()
    }

}