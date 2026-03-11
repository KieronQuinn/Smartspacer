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
 * Touch routing summary
 * ─────────────────────
 * Scenario A — no child consumed ACTION_DOWN (e.g. user touched empty space):
 *   onInterceptTouchEvent(DOWN) → feeds detector, returns false → no child target
 *   onTouchEvent(DOWN)          → feeds detector, returns true  → card becomes target
 *   onTouchEvent(MOVE/UP)       → feeds detector
 *   If fling: onFling fires inside onTouchEvent(UP) → onHorizontalSwipe called ✓
 *
 * Scenario B — a child consumed ACTION_DOWN (e.g. user touched a clickable target/complication):
 *   onInterceptTouchEvent(DOWN)    → feeds detector, returns false → child handles DOWN
 *   onInterceptTouchEvent(MOVE/UP) → feeds detector
 *   If tap:   onFling does NOT fire → returns false → child gets ACTION_UP → click fires ✓
 *   If swipe: onFling fires inside onInterceptTouchEvent(UP) → sets [flingDetected] →
 *             onHorizontalSwipe called → onInterceptTouchEvent returns true to intercept UP →
 *             child receives ACTION_CANCEL (no click) ✓
 *
 * Set [onHorizontalSwipe] to receive page-change callbacks:
 *   `true`  = swiped left  (advance to next target)
 *   `false` = swiped right (go to previous target)
 */
class SwipeDetectingCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    var onHorizontalSwipe: ((isLeft: Boolean) -> Unit)? = null

    /**
     * Set to true inside [onFling] (which fires during [gestureDetector].onTouchEvent called from
     * [onInterceptTouchEvent] for ACTION_UP).  We then return true from [onInterceptTouchEvent] to
     * intercept that ACTION_UP, causing the child to receive ACTION_CANCEL instead so its click
     * listener is never triggered by what was intended as a swipe.
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
     * Called before children receive the event (and for all events when a child touch target
     * exists).  We feed the gesture detector here and — if [onFling] fired on this same ACTION_UP
     * — return true to intercept so the child gets ACTION_CANCEL instead of ACTION_UP, preventing
     * an accidental click when the user meant to swipe.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) flingDetected = false
        gestureDetector.onTouchEvent(ev)
        if (flingDetected && ev.actionMasked == MotionEvent.ACTION_UP) {
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
     * Ignore children's requests to disallow interception — without this, an interactive child
     * calling requestDisallowInterceptTouchEvent(true) would stop [onInterceptTouchEvent] from
     * seeing subsequent MOVE/UP events, breaking swipe detection.
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}
