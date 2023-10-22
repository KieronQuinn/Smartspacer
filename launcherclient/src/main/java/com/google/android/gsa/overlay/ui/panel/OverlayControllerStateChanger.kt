package com.google.android.gsa.overlay.ui.panel

import android.annotation.SuppressLint
import android.os.Build.VERSION
import android.util.Log
import com.google.android.gsa.overlay.base.SlidingPanelLayoutDragCallback
import com.google.android.gsa.overlay.controllers.OverlayController

@SuppressLint("WrongConstant")
internal class OverlayControllerStateChanger(private val overlayController: OverlayController) :
    SlidingPanelLayoutDragCallback {
    override fun drag() {
        var overlayControllerVar = overlayController
        val panelStateVar = PanelState.DRAGGING //Todo: PanelState.uof was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
        overlayControllerVar = overlayController
        val attributes = overlayControllerVar.window!!.attributes
        if (VERSION.SDK_INT >= 26) {
            val f = attributes.alpha
            attributes.alpha = 1.0f
            if (f != attributes.alpha) {
                overlayControllerVar.window!!.attributes = attributes
                return
            }
            return
        }
        attributes.x = 0
        attributes.flags = attributes.flags and -513
        overlayControllerVar.unZ = true
        overlayControllerVar.window!!.attributes = attributes
    }

    override fun onPanelOpening() {
        var overlayControllerVar = overlayController
        val panelStateVar = PanelState.DRAGGING //Todo: PanelState.uof was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
        overlayController.setVisible(true)
        overlayControllerVar = overlayController
        val attributes = overlayControllerVar.window!!.attributes
        if (VERSION.SDK_INT >= 26) {
            val f = attributes.alpha
            attributes.alpha = 1.0f
            if (f != attributes.alpha) {
                overlayControllerVar.window!!.attributes = attributes
                return
            }
            return
        }
        attributes.x = 0
        attributes.flags = attributes.flags and -513
        overlayControllerVar.unZ = true
        overlayControllerVar.window!!.attributes = attributes
    }

    override fun onPanelClosing(fromTouch: Boolean) {
        if (fromTouch) {
            overlayController.onPanelClosing()
        }
        val overlayControllerVar = overlayController
        val panelStateVar = PanelState.DRAGGING //Todo: PanelState.uof was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
        overlayController.setVisible(false)
    }

    override fun onPanelOpen() {
        val overlayControllerVar = overlayController
        val panelStateVar = PanelState.OPEN_AS_DRAWER //Todo: PanelState.uog was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
        overlayControllerVar.onPanelOpen()
    }

    override fun onDragProgress(f: Float) {
        if (overlayController.uoa != null && !java.lang.Float.isNaN(f)) {
            try {
                overlayController.uoa!!.overlayScrollChanged(f)
                overlayController.onDragProgress(f)
            } catch (e: Throwable) {
                Log.e("wo.OverlayController", "Error notfying client", e)
            }
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
        overlayControllerVar = overlayController
        val panelStateVar = PanelState.CLOSED //Todo: PanelState.uoe was default
        if (overlayControllerVar.panelState != panelStateVar) {
            overlayControllerVar.panelState = panelStateVar
            overlayControllerVar.setState(overlayControllerVar.panelState)
        }
    }

    override fun isTransparent(): Boolean {
        return overlayController.isTransparent()
    }
}