package com.google.android.gsa.overlay.ui.panel

import android.graphics.Rect
import android.view.MotionEvent
import com.google.android.gsa.overlay.controllers.OverlayController

class OverlayControllerSlidingPanelLayout(private val overlayController: OverlayController?) :
    SlidingPanelLayout(
        overlayController
    ) {
    override fun determineScrollingStart(motionEvent: MotionEvent, f: Float) {
        var obj: Any? = 1
        if (motionEvent.findPointerIndex(mActivePointerId) != -1) {
            val x = motionEvent.x - mDownX
            var abs = Math.abs(x)
            val abs2 = Math.abs(motionEvent.y - mDownY)
            if (java.lang.Float.compare(abs, 0.0f) != 0) {
                abs = Math.atan((abs2 / abs).toDouble()).toFloat()
                val obj2: Any?
                obj2 = if (mIsRtl) {
                    if (x < 0.0f) 1 else null
                } else if (x > 0.0f) {
                    1 //TODO: different from source
                } else {
                    null
                }
                if (!mIsPanelOpen || mIsPageMoving) {
                    obj = null
                }
                if (obj != null && obj2 != null) { //TODO: different from source
                    return
                }
                if (obj != null && dragCallback!!.isTransparent() || abs > 1.0471976f) {
                    return
                }
                if (abs > 0.5235988f) {
                    super.determineScrollingStart(
                        motionEvent, Math.sqrt(((abs - 0.5235988f) / 0.5235988f).toDouble())
                            .toFloat() * 4.0f + 1.0f
                    )
                } else {
                    super.determineScrollingStart(motionEvent, f)
                }
            }
        }
    }

    override fun fitSystemWindows(rect: Rect): Boolean {
        return !overlayController!!.unZ || super.fitSystemWindows(rect)
    }
}