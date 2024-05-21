package com.google.android.gsa.overlay.callbacks

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import android.util.Log
import android.util.Pair
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.gsa.overlay.base.BaseCallback
import com.google.android.gsa.overlay.binders.OverlayControllerBinder
import com.google.android.gsa.overlay.controllers.OverlayController
import com.google.android.gsa.overlay.model.ByteBundleHolder
import com.google.android.gsa.overlay.ui.panel.OverlayControllerSlidingPanelLayout
import com.google.android.gsa.overlay.ui.panel.PanelState
import com.google.android.gsa.overlay.ui.panel.SlidingPanelLayout
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback
import java.io.PrintWriter

@SuppressLint("WrongConstant")
abstract class OverlayControllerCallback(
    val overlayControllerBinder: OverlayControllerBinder,
    val uor: Int
) : BaseCallback() {
    var overlayController: OverlayController? = null
    abstract fun createController(configuration: Configuration?): OverlayController?
    override fun handleMessage(message: Message): Boolean {
        var z = false
        val overlayControllerVar: OverlayController?
        return when (message.what) {
            0 -> {
                if (message.arg1 == 0) {
                    return true
                }
                var overlayControllerVar2: OverlayController?
                val bundle: Bundle?
                if (overlayController != null) {
                    overlayControllerVar2 = overlayController
                    val bundle2 = Bundle()
                    if (overlayControllerVar2!!.panelState == PanelState.OPEN_AS_DRAWER) { //Todo: PanelState.uog was default
                        bundle2.putBoolean("open", true)
                    }
                    bundle2.putParcelable(
                        "view_state",
                        overlayControllerVar2.window!!.saveHierarchyState()
                    )
                    overlayController?.onSaveInstanceState(bundle2)
                    overlayController!!.destroy(false)
                    overlayController = null
                    bundle = bundle2
                } else {
                    bundle = null
                }
                val pair = message.obj as Pair<*, *>
                val layoutParams =
                    (pair.first as Bundle).getParcelable<WindowManager.LayoutParams>("layout_params")
                overlayController =
                    createController((pair.first as Bundle).getParcelable<Parcelable>("configuration") as Configuration?)
                return try {
                    val i: Int
                    val overlayControllerVar3 = overlayController
                    val str = overlayControllerBinder.mPackageName
                    val bundle3 = pair.first as Bundle
                    overlayControllerVar3!!.mIsRtl = SlidingPanelLayout.isRtl(
                        overlayControllerVar3.resources
                    )
                    overlayControllerVar3.mPackageName = str
                    overlayControllerVar3.window!!.setWindowManager(
                        null, layoutParams!!.token, ComponentName(
                            overlayControllerVar3, overlayControllerVar3.baseContext.javaClass
                        ).flattenToShortString(), true
                    )
                    overlayControllerVar3.windowManager =
                        overlayControllerVar3.window!!.windowManager
                    val point = Point()
                    overlayControllerVar3.windowManager!!.defaultDisplay.getRealSize(point)
                    overlayControllerVar3.mWindowShift = -Math.max(point.x, point.y)
                    overlayControllerVar3.slidingPanelLayout =
                        OverlayControllerSlidingPanelLayout(overlayControllerVar3)
                    overlayControllerVar3.container = FrameLayout(overlayControllerVar3)
                    overlayControllerVar3.slidingPanelLayout!!.el(overlayControllerVar3.container)
                    overlayControllerVar3.slidingPanelLayout!!.dragCallback = overlayControllerVar3.overlayControllerStateChanger
                    layoutParams.width = -1
                    layoutParams.height = -1
                    layoutParams.flags = layoutParams.flags or 8650752
                    layoutParams.dimAmount = 0.0f
                    layoutParams.gravity = 3
                    i = if (VERSION.SDK_INT >= 25) {
                        4
                    } else {
                        2
                    }
                    layoutParams.type = i
                    layoutParams.softInputMode = 16
                    overlayControllerVar3.window.attributes = layoutParams
                    overlayControllerVar3.window.clearFlags(1048576)
                    //CHANGE: bundle3 -> bundle as bundle3 wasn't being used but bundle will be useful
                    overlayControllerVar3.onCreate(bundle)
                    overlayControllerVar3.onRestoreInstanceState(bundle)
                    overlayControllerVar3.window.setContentView(overlayControllerVar3.slidingPanelLayout)
                    overlayControllerVar3.windowView = overlayControllerVar3.window.decorView
                    overlayControllerVar3.windowManager!!.addView(
                        overlayControllerVar3.windowView,
                        overlayControllerVar3.window.attributes
                    )
                    overlayControllerVar3.setVisible(false)
                    overlayControllerVar3.window.decorView.addOnLayoutChangeListener(
                        OverlayControllerLayoutChangeListener(overlayControllerVar3)
                    )
                    if (bundle != null) {
                        overlayControllerVar = overlayController
                        overlayControllerVar!!.window!!.restoreHierarchyState(bundle.getBundle("view_state"))
                        if (bundle.getBoolean("open")) {
                            val slidingPanelLayoutVar = overlayControllerVar.slidingPanelLayout
                            slidingPanelLayoutVar!!.mPanelPositionRatio = 1.0f
                            slidingPanelLayoutVar.panelX = slidingPanelLayoutVar.measuredWidth
                            slidingPanelLayoutVar.uoA!!.translationX =
                                if (slidingPanelLayoutVar.mIsRtl) (-slidingPanelLayoutVar.panelX).toFloat() else slidingPanelLayoutVar.panelX.toFloat()
                            slidingPanelLayoutVar.cnF()
                            slidingPanelLayoutVar.cnG()
                        }
                    }
                    overlayControllerVar2 = overlayController
                    overlayControllerVar2!!.uoa = pair.second as ILauncherOverlayCallback
                    overlayControllerVar2.bP(true)
                    overlayControllerBinder.windowAttached(
                        pair.second as ILauncherOverlayCallback,
                        uor
                    )
                    true
                } catch (e: Throwable) {
                    Log.e("OverlaySController", "Error creating overlay window", e)
                    val obtain = Message.obtain()
                    obtain.what = 2
                    handleMessage(obtain)
                    obtain.recycle()
                    true
                }
                if (overlayController == null) {
                    return true
                }
                overlayController!!.BJ(message.obj as Int)
                true
            }
            1 -> {
                if (overlayController == null) {
                    return true
                }
                overlayController!!.BJ(message.obj as Int)
                true
            }
            2 -> {
                if (overlayController == null) {
                    return true
                }
                val cnC = overlayController!!.destroy(true)
                overlayController = null
                if (message.arg1 != 0) {
                    return true
                }
                overlayControllerBinder.windowAttached(cnC, 0)
                true
            }
            6 -> {
                if (overlayController == null) {
                    return true
                }
                val i2 = message.arg2 and 1
                if (message.arg1 == 1) {
                    overlayController!!.BK(i2)
                    return true
                }
                overlayController!!.onHomeOrBackPressed(i2)
                true
            }
            7 -> {
                if (overlayController == null) {
                    return true
                }
                overlayControllerVar = overlayController
                if (message.arg1 == 1) {
                    z = true
                }
                overlayControllerVar!!.bP(z)
                true
            }
            8 -> {
                if (overlayController == null) {
                    return true
                }
                overlayController!!.a(message.obj as ByteBundleHolder)
                true
            }
            else -> false
        }
    }

    override fun dump(printWriter: PrintWriter, str: String) {
        val overlayControllerVar = overlayController
        val valueOf = overlayControllerVar.toString()
        printWriter.println(
            StringBuilder(str.length + 8 + valueOf.length).append(str).append(" mView: ")
                .append(valueOf).toString()
        )
        overlayControllerVar?.dump(printWriter, "$str  ")
    }
}