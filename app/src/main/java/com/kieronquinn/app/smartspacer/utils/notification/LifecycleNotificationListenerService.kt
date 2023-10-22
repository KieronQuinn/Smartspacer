package com.kieronquinn.app.smartspacer.utils.notification

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher

abstract class LifecycleNotificationListenerService: NotificationListenerService(), LifecycleOwner {

    private val mDispatcher by lazy {
        ServiceLifecycleDispatcher(this)
    }

    override val lifecycle
        get() = mDispatcher.lifecycle

    @CallSuper
    override fun onListenerConnected() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onListenerConnected()
    }

    @CallSuper
    override fun onListenerDisconnected() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onListenerDisconnected()
    }

    @Deprecated("Deprecated in Java")
    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        mDispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

}