package com.google.android.gsa.overlay.callbacks

import android.annotation.SuppressLint
import android.os.Build.VERSION
import android.view.View
import com.google.android.gsa.overlay.ui.panel.PanelState
import com.google.android.gsa.overlay.controllers.OverlayController

@SuppressLint("WrongConstant")
internal class OverlayControllerLayoutChangeListener(private val overlayController: OverlayController?) :
    View.OnLayoutChangeListener {
    override fun onLayoutChange(
        view: View,
        i: Int,
        i2: Int,
        i3: Int,
        i4: Int,
        i5: Int,
        i6: Int,
        i7: Int,
        i8: Int
    ) {
        overlayController!!.window!!.decorView.removeOnLayoutChangeListener(this)
        if (overlayController.panelState == PanelState.CLOSED) { //Todo: PanelState.uoe was default
            val overlayControllerVar = overlayController
            val attributes = overlayControllerVar.window!!.attributes
            if (VERSION.SDK_INT >= 26) {
                val f = attributes.alpha
                attributes.alpha = 0.0f
                if (f != attributes.alpha) {
                    overlayControllerVar.window!!.attributes = attributes
                    return
                }
                return
            }
            attributes.x = overlayControllerVar.mWindowShift
            attributes.flags = attributes.flags or 512
            overlayControllerVar.unZ = false
            overlayControllerVar.window!!.attributes = attributes
        }
    }
}