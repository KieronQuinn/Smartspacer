package com.google.android.gsa.overlay.callbacks

import android.content.res.Configuration
import android.os.Message
import com.google.android.gsa.overlay.binders.OverlayControllerBinder
import com.google.android.gsa.overlay.controllers.OverlayController
import com.google.android.gsa.overlay.controllers.OverlaysController
import java.io.PrintWriter

class MinusOneOverlayCallback(
    private val overlaysController: OverlaysController,
    overlayControllerBinderVar: OverlayControllerBinder
) : OverlayControllerCallback(overlayControllerBinderVar, 3) {
    override fun createController(configuration: Configuration?): OverlayController? {
        return overlaysController.createController(
            configuration,
            overlayControllerBinder.mCallerUid,
            overlayControllerBinder.mServerVersion,
            overlayControllerBinder.mClientVersion
        )
    }

    override fun dump(printWriter: PrintWriter, str: String) {
        printWriter.println(str + "MinusOneOverlayCallback")
        super.dump(printWriter, str)
    }

    override fun handleMessage(message: Message): Boolean {
        if (super.handleMessage(message)) {
            return true
        }
        val overlayControllerVar: OverlayController?
        val `when`: Long
        return when (message.what) {
            3 -> {
                if (overlayController != null) {
                    overlayControllerVar = overlayController
                    `when` = message.getWhen()
                    if (!overlayControllerVar!!.isOpenState()) {
                        val slidingPanelLayoutVar = overlayControllerVar.slidingPanelLayout
                        if (slidingPanelLayoutVar!!.panelX < slidingPanelLayoutVar.mTouchSlop) {
                            overlayControllerVar.slidingPanelLayout!!.BM(0)
                            overlayControllerVar.mAcceptExternalMove = true
                            overlayControllerVar.scrollX = 0
                            overlayControllerVar.slidingPanelLayout!!.mForceDrag = true
                            overlayControllerVar.obZ = `when` - 30
                            overlayControllerVar.sendTouchEvent(
                                0,
                                overlayControllerVar.scrollX,
                                overlayControllerVar.obZ
                            )
                            overlayControllerVar.sendTouchEvent(2, overlayControllerVar.scrollX, `when`)
                        }
                    }
                }
                true
            }
            4 -> {
                if (overlayController != null) {
                    overlayControllerVar = overlayController
                    val floatValue = message.obj as Float
                    `when` = message.getWhen()
                    if (overlayControllerVar!!.mAcceptExternalMove) {
                        overlayControllerVar.scrollX =
                            (floatValue * overlayControllerVar.slidingPanelLayout!!.measuredWidth
                                .toFloat()).toInt()
                        overlayControllerVar.sendTouchEvent(2, overlayControllerVar.scrollX, `when`)
                    }
                }
                true
            }
            5 -> {
                if (overlayController != null) {
                    overlayControllerVar = overlayController
                    `when` = message.getWhen()
                    if (overlayControllerVar!!.mAcceptExternalMove) {
                        overlayControllerVar.sendTouchEvent(1, overlayControllerVar.scrollX, `when`)
                    }
                    overlayControllerVar.mAcceptExternalMove = false
                }
                true
            }
            else -> false
        }
    }
}