package com.kieronquinn.app.smartspacer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

/**
 * A [MaterialCardView] that observes horizontal-fling gestures via [onInterceptTouchEvent]
 * without ever stealing the event from child views.
 *
 * Using [onInterceptTouchEvent] (rather than [setOnTouchListener] / [onTouchEvent]) is critical
 * because it is called for every motion event dispatched through this ViewGroup, even when a
 * child view ultimately handles the event.  The gesture detector therefore reliably sees the
 * complete ACTION_DOWN … ACTION_MOVE … ACTION_UP sequence regardless of child interactivity.
 *
 * Set [onHorizontalSwipe] to receive callbacks: `true` = swipe-left (next), `false` = swipe-right
 * (previous).
 */
class SwipeDetectingCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    var onHorizontalSwipe: ((isLeft: Boolean) -> Unit)? = null

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            // onDown MUST return true — without it the detector discards all subsequent
            // events in the same gesture sequence and onFling never fires.
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
                onHorizontalSwipe?.invoke(dx < 0)
                return true
            }
        })

    /**
     * Called before children receive the event — we observe but always return false so children
     * keep full interactivity (taps, long-presses, ripples, etc.).
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return false
    }
}
