package com.kieronquinn.app.smartspacer.utils.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

abstract class LifecycleAccessibilityService: AccessibilityService(), LifecycleOwner {

    private val mDispatcher by lazy {
        ServiceLifecycleDispatcher(this)
    }

    override val lifecycle
        get() = mDispatcher.lifecycle

    @CallSuper
    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        //No bind in accessibility service, but it will already be bound so we can just fire it now
        mDispatcher.onServicePreSuperOnBind()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

}