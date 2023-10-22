package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.NestedScrollingParent
import androidx.recyclerview.widget.RecyclerView

/**
 * 	A [RecyclerView] with [NestedScrollingParent] implemented, to allow
 * 	child nested scroll elements
 *
 * 	Based on https://github.com/Widgetlabs/expedition-nestedscrollview
 */
open class NestedRecyclerView : LifecycleAwareRecyclerView, NestedScrollingParent {

	private var nestedScrollTarget: View? = null
	private var nestedScrollTargetIsBeingDragged = false
	private var nestedScrollTargetWasUnableToScroll = false
	private var skipsTouchInterception = false

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) :
		super(context, attrs)


	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
		super(context, attrs, defStyleAttr)


	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		val temporarilySkipsInterception = nestedScrollTarget != null
		if (temporarilySkipsInterception) {
			// If a descendent view is scrolling we set a flag to temporarily skip our onInterceptTouchEvent implementation
			skipsTouchInterception = true
		}

		// First dispatch, potentially skipping our onInterceptTouchEvent
		var handled = try {
			super.dispatchTouchEvent(ev)
		}catch (e: NullPointerException){
			//Flexbox issue
			false
		}

		if (temporarilySkipsInterception) {
			skipsTouchInterception = false

			// If the first dispatch yielded no result or we noticed that the descendent view is unable to scroll in the
			// direction the user is scrolling, we dispatch once more but without skipping our onInterceptTouchEvent.
			// Note that RecyclerView automatically cancels active touches of all its descendents once it starts scrolling
			// so we don't have to do that.
			if (!handled || nestedScrollTargetWasUnableToScroll) {
				handled = super.dispatchTouchEvent(ev)
			}
		}

		return handled
	}


	// Skips RecyclerView's onInterceptTouchEvent if requested
	override fun onInterceptTouchEvent(e: MotionEvent) =
		!skipsTouchInterception && super.onInterceptTouchEvent(e)


	override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
		if (target === nestedScrollTarget && !nestedScrollTargetIsBeingDragged) {
			if (dyConsumed != 0) {
				// The descendent was actually scrolled, so we won't bother it any longer.
				// It will receive all future events until it finished scrolling.
				nestedScrollTargetIsBeingDragged = true
				nestedScrollTargetWasUnableToScroll = false
			}
			else if (dyConsumed == 0 && dyUnconsumed != 0) {
				// The descendent tried scrolling in response to touch movements but was not able to do so.
				// We remember that in order to allow RecyclerView to take over scrolling.
				nestedScrollTargetWasUnableToScroll = true
				target.parent?.requestDisallowInterceptTouchEvent(false)
			}
		}
	}


	override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
		if (axes and View.SCROLL_AXIS_VERTICAL != 0) {
			// A descendent started scrolling, so we'll observe it.
			nestedScrollTarget = target
			nestedScrollTargetIsBeingDragged = false
			nestedScrollTargetWasUnableToScroll = false
		}

		super.onNestedScrollAccepted(child, target, axes)
	}


	// We only support vertical scrolling.
	override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int) =
		(nestedScrollAxes and View.SCROLL_AXIS_VERTICAL != 0)


	override fun onStopNestedScroll(child: View) {
		// The descendent finished scrolling. Clean up!
		nestedScrollTarget = null
		nestedScrollTargetIsBeingDragged = false
		nestedScrollTargetWasUnableToScroll = false
	}

}