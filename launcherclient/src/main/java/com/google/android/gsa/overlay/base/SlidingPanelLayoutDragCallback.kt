package com.google.android.gsa.overlay.base

import com.google.android.gsa.overlay.controllers.TransparentOverlayController
import com.google.android.gsa.overlay.ui.panel.OverlayControllerStateChanger

interface SlidingPanelLayoutDragCallback {
    fun onDragProgress(f: Float)
    fun drag()
    fun onPanelOpening()
    fun onPanelOpen()
    fun onPanelClose()
    /**
     *  Not sure what this really does, but the only visible difference is that it's true in
     *  [TransparentOverlayController] and false in [OverlayControllerStateChanger]
     */
    fun isTransparent(): Boolean
    fun onPanelClosing(fromTouch: Boolean)
}