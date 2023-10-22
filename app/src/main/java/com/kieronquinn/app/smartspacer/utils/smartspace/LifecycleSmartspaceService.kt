package com.kieronquinn.app.smartspacer.utils.smartspace

import android.content.Intent
import android.service.smartspace.SmartspaceService
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 *  LifecycleService equivalent for [SmartspaceService], all the same except that it does not
 *  implement [ServiceLifecycleDispatcher.onServicePreSuperOnBind] as we cannot override onBind.
 */
abstract class LifecycleSmartspaceService: SmartspaceService(), LifecycleOwner {

    private val mDispatcher by lazy {
        ServiceLifecycleDispatcher(this)
    }

    override val lifecycle
        get() = mDispatcher.lifecycle

    @CallSuper
    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @Deprecated("Deprecated in Java")
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