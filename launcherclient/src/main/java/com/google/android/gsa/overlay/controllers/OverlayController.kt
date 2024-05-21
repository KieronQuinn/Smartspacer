package com.google.android.gsa.overlay.controllers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import com.google.android.gsa.overlay.base.SlidingPanelLayoutDragCallback
import com.google.android.gsa.overlay.model.ByteBundleHolder
import com.google.android.gsa.overlay.ui.panel.OverlayControllerStateChanger
import com.google.android.gsa.overlay.ui.panel.PanelState
import com.google.android.gsa.overlay.ui.panel.SlidingPanelLayout
import java.io.PrintWriter

open class OverlayController(context: Context?, theme: Int, dialogTheme: Int) :
    DialogOverlayController(context, theme, dialogTheme) {
    var mIsRtl = false
    var obZ: Long = 0
    var mWindowShift = 0
    var mPackageName: String? = null
    var slidingPanelLayout: SlidingPanelLayout? = null
    var overlayControllerStateChanger: SlidingPanelLayoutDragCallback =
        OverlayControllerStateChanger(this)
    var container: FrameLayout? = null
    var scrollX = 0
    var mAcceptExternalMove = false
    var unZ = true
    var uoa: com.google.android.libraries.launcherclient.ILauncherOverlayCallback? = null
    var panelState = PanelState.CLOSED
    var mActivityStateFlags = 0

    fun sendTouchEvent(i: Int, i2: Int, j: Long) {
        val obtain =
            MotionEvent.obtain(obZ, j, i, if (mIsRtl) (-i2).toFloat() else i2.toFloat(), 0.0f, 0)
        obtain.source = 4098
        slidingPanelLayout!!.dispatchTouchEvent(obtain)
        obtain.recycle()
    }

    fun destroy(isFinishing: Boolean): com.google.android.libraries.launcherclient.ILauncherOverlayCallback? {
        BJ(0)
        try {
            windowView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Throwable) {
            Log.e("wo.OverlayController", "Error removing overlay window", e)
        }
        windowView = null
        cnB()
        onDestroy(isFinishing)
        return uoa
    }

    fun BJ(i: Int) {
        var i2 = 1
        var i3 = 0
        if (mActivityStateFlags != i) {
            val i4: Int
            var i5: Int
            val i6: Int
            val i7 = if (mActivityStateFlags and 1 != 0) 1 else 0
            i4 = if (mActivityStateFlags and 2 != 0) {
                1
            } else {
                0
            }
            i5 = if (i and 1 != 0) {
                1
            } else {
                0
            }
            i6 = if (i and 2 != 0) {
                1
            } else {
                0
            }
            i5 = if (i5 == 0 && i6 == 0) {
                0
            } else {
                1
            }
            if (i5 == 0) {
                i2 = 0
            }
            if (i6 != 0) {
                i3 = 2
            }
            mActivityStateFlags = i2 or i3
            if (i7 == 0 && i5 != 0) {
                onStart()
            }
            if (i7 != 0 && i5 == 0) {
                onStop()
            }
            if(i == 0) {
                onPause()
            }
            if(i == 3) {
                onResume()
            }
        }
    }

    @CallSuper
    open fun onHomeOrBackPressed(i: Int) {
        var i2 = 1
        var i3 = 0
        if (isOpenState()) {
            //Panel is closing from home button or back press
            onPanelClosing()
            var i4 = if (i and 1 != 0) 1 else 0
            if (panelState == PanelState.OPEN_AS_LAYER) {
                i2 = 0
            }
            i4 = i4 and i2
            val slidingPanelLayoutVar = slidingPanelLayout
            if (i4 != 0) {
                i3 = 750
            }
            slidingPanelLayoutVar!!.closePanel(i3)
            cnB()
        }
    }

    fun closePanel(){
        slidingPanelLayout?.closePanel(750)
    }

    fun BK(i: Int) {
        var i2 = 0
        if (panelState == PanelState.CLOSED) {
            var i3 = if (i and 1 != 0) 1 else 0
            if (i and 2 != 0) {
                slidingPanelLayout!!.dragCallback = TransparentOverlayController(this)
                i3 = 0
            }
            val slidingPanelLayoutVar = slidingPanelLayout
            if (i3 != 0) {
                i2 = 750
            }
            slidingPanelLayoutVar!!.fv(i2)
        }
    }

    fun a(byteBundleHolderVar: ByteBundleHolder?) {}
    override fun onBackPressed() {
        onHomeOrBackPressed(1)
    }

    fun dump(printWriter: PrintWriter, str: String) {
        printWriter.println(
            StringBuilder(str.length + 25).append(str).append("mWindowShift: ").append(
                mWindowShift
            ).toString()
        )
        printWriter.println(
            StringBuilder(str.length + 26).append(str).append("mAcceptExternalMove: ").append(
                mAcceptExternalMove
            ).toString()
        )
        var valueOf = panelState.toString()
        printWriter.println(
            StringBuilder(str.length + 14 + valueOf.length).append(str).append("mDrawerState: ")
                .append(valueOf).toString()
        )
        printWriter.println(
            StringBuilder(str.length + 32).append(str).append("mActivityStateFlags: ").append(
                mActivityStateFlags
            ).toString()
        )
        valueOf = slidingPanelLayout.toString()
        printWriter.println(
            StringBuilder(str.length + 14 + valueOf.length).append(str).append("mWrapperView: ")
                .append(valueOf).toString()
        )
        val slidingPanelLayoutVar = slidingPanelLayout
        val concat = "$str  "
        printWriter.println(
            StringBuilder(concat.length + 36).append(concat).append("mPanelPositionRatio: ").append(
                slidingPanelLayoutVar!!.mPanelPositionRatio
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 23).append(concat).append("mDownX: ").append(
                slidingPanelLayoutVar.mDownX
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 23).append(concat).append("mDownY: ").append(
                slidingPanelLayoutVar.mDownY
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 29).append(concat).append("mActivePointerId: ").append(
                slidingPanelLayoutVar.mActivePointerId
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 24).append(concat).append("mTouchState: ").append(
                slidingPanelLayoutVar.mTouchState
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 19).append(concat).append("mIsPanelOpen: ").append(
                slidingPanelLayoutVar.mIsPanelOpen
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 20).append(concat).append("mIsPageMoving: ").append(
                slidingPanelLayoutVar.mIsPageMoving
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 16).append(concat).append("mSettling: ").append(
                slidingPanelLayoutVar.mSettling
            ).toString()
        )
        printWriter.println(
            StringBuilder(concat.length + 17).append(concat).append("mForceDrag: ").append(
                slidingPanelLayoutVar.mForceDrag
            ).toString()
        )
    }

    open fun onPanelClosing() {}
    open fun onCreate(bundle: Bundle?) {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onStart() {}
    open fun onResume() {}
    open fun onSaveInstanceState(bundle: Bundle){}
    open fun onRestoreInstanceState(bundle: Bundle?){}

    @CallSuper
    open fun onPanelOpen() {
        //onResume()
    }

    fun setTitle(charSequence: CharSequence?) {
        this.window!!.setTitle(charSequence)
    }

    open fun onDestroy(isFinishing: Boolean) {}

    fun bP(z: Boolean) {}

    fun isTransparent(): Boolean {
        return false
    }

    open fun onDragProgress(progress: Float) {}

    override fun getSystemService(str: String): Any {
        return if ("window" != str || windowManager == null) {
            super.getSystemService(str)
        } else windowManager!!
    }

    fun isOpenState(): Boolean {
        return panelState == PanelState.OPEN_AS_DRAWER || panelState == PanelState.OPEN_AS_LAYER
    }

    fun setVisible(z: Boolean) {
        if (z) {
            this.window!!.clearFlags(24)
        } else {
            this.window!!.addFlags(24)
        }
    }

    fun setState(panelStateVar: PanelState?) {
        when(panelStateVar) {
            PanelState.OPEN_AS_LAYER,
            PanelState.OPEN_AS_DRAWER -> {
                onResume()
            }
            PanelState.CLOSED -> {
                onPause()
            }
            else -> {
                //No-op
            }
        }
    }
}