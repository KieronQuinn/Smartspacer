package com.google.android.gsa.overlay.binders

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Pair
import android.view.WindowManager
import com.google.android.gsa.overlay.base.BaseCallback
import com.google.android.gsa.overlay.callbacks.MinusOneOverlayCallback
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.google.android.libraries.launcherclient.ILauncherOverlay
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback

class OverlayControllerBinder(
    private val overlaysController: OverlaysController,
    val mCallerUid: Int,
    val mPackageName: String?,
    val mServerVersion: Int,
    val mClientVersion: Int,
    private val originalBinder: ILauncherOverlay?
) : ILauncherOverlay.Stub(), Runnable {

    var mOptions = 0
    var baseCallback = BaseCallback()
    private var mainThreadHandler = Handler(Looper.getMainLooper(), baseCallback)
    var mLastAttachWasLandscape = false

    private fun checkCallerId() {
        if (getCallingUid() != mCallerUid) {
            throw RuntimeException("Invalid client")
        }
    }

    @Synchronized
    override fun startScroll() {
        checkCallerId()
        Message.obtain(mainThreadHandler, 3).sendToTarget()
    }

    @Synchronized
    override fun onScroll(progress: Float) {
        checkCallerId()
        Message.obtain(mainThreadHandler, 4, progress).sendToTarget()
    }

    @Synchronized
    override fun endScroll() {
        checkCallerId()
        Message.obtain(mainThreadHandler, 5).sendToTarget()
    }

    @Synchronized
    override fun windowAttached(
        layoutParams: WindowManager.LayoutParams?,
        callback: ILauncherOverlayCallback,
        clientOptions: Int
    ) {
        val bundle = Bundle()
        bundle.putParcelable("layout_params", layoutParams)
        bundle.putInt("client_options", clientOptions)
        windowAttached2(bundle, callback)
    }

    @Synchronized
    override fun windowAttached2(bundle: Bundle, callback: ILauncherOverlayCallback) {
        checkCallerId()
        overlaysController.handler.removeCallbacks(this)
        val configuration = bundle.getParcelable<Configuration>("configuration")
        mLastAttachWasLandscape =
            configuration != null && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        createCallback(bundle.getInt("client_options", 7))
        Message.obtain(mainThreadHandler, 0, 1, 0, Pair.create(bundle, callback)).sendToTarget()
    }

    @Synchronized
    override fun windowDetached(isChangingConfigurations: Boolean) {
        checkCallerId()
        Message.obtain(mainThreadHandler, 0, 0, 0).sendToTarget()
        overlaysController.handler.postDelayed(this, if (isChangingConfigurations) 5000 else 0.toLong())
    }

    @Synchronized
    override fun setActivityState(i: Int) {
        checkCallerId()
        mainThreadHandler.removeMessages(1)
        if (i and 2 == 0) {
            mainThreadHandler.sendMessageDelayed(Message.obtain(mainThreadHandler, 1, i), 100)
        } else {
            Message.obtain(mainThreadHandler, 1, i).sendToTarget()
        }
        originalBinder?.setActivityState(i)
    }

    @Synchronized
    override fun onPause() {
        setActivityState(0)
        originalBinder?.onPause()
    }

    @Synchronized
    private fun createCallback(clientOptions: Int) {
        synchronized(this) {
            var i2 = clientOptions and 15
            if (i2 and 1 != 0) {
                i2 = 1
            }
            if (mOptions != i2) {
                mainThreadHandler.removeCallbacksAndMessages(null)
                Message.obtain(mainThreadHandler, 0, 0, 0).sendToTarget()
                sendBoolean(true)
                mOptions = i2
                val baseCallbackVar = when (mOptions) {
                    1 -> MinusOneOverlayCallback(overlaysController, this)
                    else -> BaseCallback()
                }
                baseCallback = baseCallbackVar
                mainThreadHandler = Handler(Looper.getMainLooper(), baseCallback)
            }
        }
    }

    @Synchronized
    override fun onResume() {
        setActivityState(3)
        originalBinder?.onResume()
    }

    @Synchronized
    override fun closeOverlay(flags: Int) {
        checkCallerId()
        mainThreadHandler.removeMessages(6)
        Message.obtain(mainThreadHandler, 6, 0, flags).sendToTarget()
    }

    @Synchronized
    override fun openOverlay(flags: Int) {
        checkCallerId()
        mainThreadHandler.removeMessages(6)
        Message.obtain(mainThreadHandler, 6, 1, flags).sendToTarget()
    }

    override fun startSearch(data: ByteArray, bundle: Bundle): Boolean {
        return originalBinder?.startSearch(data, bundle) ?: false
    }

    override fun unusedMethod() {
    }

    @Synchronized
    override fun requestVoiceDetection(start: Boolean) {
        originalBinder?.requestVoiceDetection(start)
    }

    override fun getVoiceSearchLanguage(): String? {
        return originalBinder?.voiceSearchLanguage
    }

    override fun isVoiceDetectionRunning(): Boolean {
        return originalBinder?.isVoiceDetectionRunning ?: false
    }

    override fun hasOverlayContent(): Boolean {
        return true
    }

    override fun run() {
        destroy()
    }

    fun destroy() {
        synchronized(overlaysController) {
            overlaysController.handler.removeCallbacks(this)
            sendBoolean(false)
        }
    }

    @Synchronized
    private fun sendBoolean(value: Boolean) {
        var i = 0
        synchronized(this) {
            val handler = mainThreadHandler
            if (value) {
                i = 1
            }
            Message.obtain(handler, 2, i, 0).sendToTarget()
        }
    }

    fun windowAttached(callback: ILauncherOverlayCallback?, status: Int) {
        if (callback != null) {
            try {
                callback.overlayStatusChanged(24 or status)
            } catch (e: Throwable) {
                Log.e("OverlaySController", "Failed to send status update", e)
            }
        }
    }
}