package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

/**
 * A [MaterialCardView] that detects horizontal-fling gestures and pages through Smartspace
 * targets while keeping every interactive child (targets, complications) fully clickable.
 *
 * ## Touch routing
 *
 * **Scenario A — no child consumed ACTION_DOWN** (e.g. empty space in the pill):
 * - `onInterceptTouchEvent(DOWN)` → feeds detector, requests parent disallow, returns false
 * - No child handles → `onTouchEvent(DOWN)` → feeds detector, returns true (card = target)
 * - `onTouchEvent(MOVE/UP)` → feeds detector
 * - If fling: `onFling` fires → `onHorizontalSwipe` ✓
 *
 * **Scenario B — a child consumed ACTION_DOWN** (e.g. a clickable target or complication):
 * - `onInterceptTouchEvent(DOWN)` → feeds detector, requests parent disallow, returns false
 * - Child handles DOWN; subsequent events come back via `onInterceptTouchEvent`
 * - If **tap**:  `onFling` never fires → returns false → child gets ACTION_UP → click ✓
 * - If **swipe**: `onFling` fires inside `onInterceptTouchEvent(UP)` → sets [flingDetected] →
 *   `onHorizontalSwipe` called → returns true → child gets ACTION_CANCEL (no click) ✓
 *
 * ## Overlay / Smart Launcher interaction
 *
 * When Smartspacer is hosted as the Smart Launcher -1 feed page, user touches flow through
 * [com.google.android.gsa.overlay.ui.panel.SlidingPanelLayout] before reaching this view.
 * That layout's [OverlayControllerSlidingPanelLayout.determineScrollingStart] only suppresses
 * drag detection for *rightward* swipes when the panel is open; leftward swipes (which we use
 * for "next page") are still eligible to be stolen as "close overlay" gestures.
 *
 * Calling `parent?.requestDisallowInterceptTouchEvent(true)` on ACTION_DOWN tells every ancestor
 * ViewGroup — including `SlidingPanelLayout` — not to intercept moves or ups for this gesture.
 * This is done from [onInterceptTouchEvent], which runs *after* `SlidingPanelLayout` has already
 * decided not to intercept the DOWN itself, so the flag is set in time for the MOVE events.
 *
 * Set [onHorizontalSwipe] to receive page-change callbacks:
 * - `true`  = swiped left  (advance to next target)
 * - `false` = swiped right (go to previous target)
 */
class SwipeDetectingCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    var onHorizontalSwipe: ((isLeft: Boolean) -> Unit)? = null

    /**
     * Set to true inside [onFling] (which fires during [gestureDetector].onTouchEvent called from
     * [onInterceptTouchEvent] for ACTION_UP).  We then return true from [onInterceptTouchEvent]
     * to intercept that ACTION_UP, so the child gets ACTION_CANCEL instead of ACTION_UP,
     * preventing its click listener from firing on what was intended as a swipe.
     */
    private var flingDetected = false

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            // onDown MUST return true — without it the detector discards all subsequent
            // events in the same gesture and onFling never fires.
            override fun onDown(e: MotionEvent) = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val dx = e2.x - (e1?.x ?: return false)
                val dy = e2.y - (e1?.y ?: return false)
                if (abs(dx) < 80 || abs(velocityX) < 100 || abs(dy) >= abs(dx)) return false
                flingDetected = true
                onHorizontalSwipe?.invoke(dx < 0)
                return true
            }
        })

    /**
     * Called before children receive the event (and for all events once a child touch target
     * exists).  On ACTION_DOWN we immediately ask all ancestor ViewGroups not to intercept,
     * preventing [com.google.android.gsa.overlay.ui.panel.SlidingPanelLayout] from stealing
     * leftward swipes as "close overlay" gestures.
     *
     * If [onFling] fired on this same ACTION_UP we return true to intercept, so the child that
     * was tracking the touch gets ACTION_CANCEL instead of ACTION_UP (no click for a swipe).
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            flingDetected = false
            // Tell every ancestor (including SlidingPanelLayout in the overlay context) not to
            // intercept moves/ups for gestures that start within this pill.
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        gestureDetector.onTouchEvent(ev)
        if (onHorizontalSwipe != null && flingDetected && ev.actionMasked == MotionEvent.ACTION_UP) {
            flingDetected = false
            return true
        }
        return false
    }

    /**
     * Handles Scenario A: when no child consumed ACTION_DOWN, subsequent MOVE/UP events arrive
     * here directly (bypassing [onInterceptTouchEvent]).  We consume ACTION_DOWN so the card
     * keeps receiving those events, but skip super so no ripple or performClick fires on a tap —
     * page navigation is swipe-only at the pill level.
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) flingDetected = false
        gestureDetector.onTouchEvent(ev)
        return ev.actionMasked == MotionEvent.ACTION_DOWN
    }

    /**
     * Do NOT call super — that would set FLAG_DISALLOW_INTERCEPT on this view, preventing our
     * own [onInterceptTouchEvent] from seeing swipe events.
     *
     * DO propagate upward: in the overlay context, ancestor ViewGroups like SlidingPanelLayout
     * should be told not to intercept when a child inside the pill is handling a gesture.
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}
