package com.google.android.gsa.overlay.controllers

import android.annotation.SuppressLint
import android.os.Build.VERSION
import android.util.Log
import com.google.android.gsa.overlay.ui.panel.PanelState
import com.google.android.gsa.overlay.base.SlidingPanelLayoutDragCallback

@SuppressLint("WrongConstant")
internal class TransparentOverlayController(private val overlayController: OverlayController) :
    SlidingPanelLayoutDragCallback {
    override fun drag() {
        Log.d("wo.OverlayController", "Drag event called in transparent mode")
    }

    override fun onPanelOpening() {}
    override fun onPanelClosing(fromTouch: Boolean) {}
    override fun onPanelOpen() {
        overlayController.setVisible(true)
        var overlayControllerVar = overlayController
        val attributes = overlayControllerVar.window!!.attributes
        if (VERSION.SDK_INT >= 26) {
            val f = attributes.alpha
            attributes.alpha = 1.0f
            if (f != attributes.alpha) {
                overlayControllerVar.window!!.attributes = attributes
            }
        } else {
            attributes.x = 0
            attributes.flags = attributes.flags and -513
            overlayControllerVar.unZ = true
            overlayControllerVar.window!!.attributes = attributes
        }
        overlayControllerVar = overlayController
        val panelStateVar = PanelState.OPEN_AS_LAYER //Todo: PanelState.uoh was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
    }

    override fun onPanelClose() {
        var overlayControllerVar = overlayController
        val attributes = overlayControllerVar.window!!.attributes
        if (VERSION.SDK_INT >= 26) {
            val f = attributes.alpha
            attributes.alpha = 0.0f
            if (f != attributes.alpha) {
                overlayControllerVar.window!!.attributes = attributes
            }
        } else {
            attributes.x = overlayControllerVar.mWindowShift
            attributes.flags = attributes.flags or 512
            overlayControllerVar.unZ = false
            overlayControllerVar.window!!.attributes = attributes
        }
        overlayController.setVisible(false)
        overlayControllerVar = overlayController
        val panelStateVar = PanelState.CLOSED //Todo: PanelState.uoe was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
        overlayController.slidingPanelLayout!!.dragCallback = overlayController.overlayControllerStateChanger
    }

    override fun onDragProgress(f: Float) {}
    override fun isTransparent(): Boolean {
        return true
    }
}