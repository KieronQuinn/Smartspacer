package com.kieronquinn.app.smartspacer.sdk.client

import android.os.Binder
import android.os.Bundle
import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Wrapper for a Smartspace session instance.
 */
class SmartspaceSession internal constructor(
    private val sessionId: SmartspaceSessionId,
    private val notifySmartspaceEvent: suspend (SmartspaceSessionId, SmartspaceTargetEvent) -> Boolean,
    private val requestSmartspaceUpdate: suspend (SmartspaceSessionId) -> Boolean,
    private val registerSmartspaceUpdates: suspend (SmartspaceSessionId, ISmartspaceCallback) -> Boolean,
    private val unregisterSmartspaceUpdates: suspend (SmartspaceSessionId, ISmartspaceCallback) -> Boolean,
    private val destroySmartspaceSession: suspend (SmartspaceSessionId) -> Unit
) {

    private val isClosed = AtomicBoolean(false)
    private val registeredListeners = HashMap<OnTargetsAvailableListener, CallbackWrapper>()

    /**
     *  Notify the Smartspace service of a [SmartspaceTargetEvent], such as resuming or pausing
     */
    suspend fun notifySmartspaceEvent(event: SmartspaceTargetEvent): Boolean {
        throwIfClosed()
        return notifySmartspaceEvent(sessionId, event)
    }

    /**
     *  Request a Smartspace update, will return data to a registered [OnTargetsAvailableListener]
     *  if new Targets are available.
     */
    suspend fun requestSmartspaceUpdate(): Boolean {
        throwIfClosed()
        return requestSmartspaceUpdate(sessionId)
    }

    /**
     *  Add a listener for new or updated Targets becoming available
     */
    suspend fun addTargetsAvailableListener(
        executor: Executor, listener: OnTargetsAvailableListener
    ): Boolean {
        throwIfClosed()
        val wrapper = CallbackWrapper(executor, listener)
        registeredListeners[listener] = wrapper
        return registerSmartspaceUpdates(sessionId, wrapper)
    }

    /**
     *  Removes a previously added Target listener
     */
    suspend fun removeTargetsAvailableListener(listener: OnTargetsAvailableListener): Boolean {
        throwIfClosed()
        val wrapper = registeredListeners[listener] ?: return true
        return unregisterSmartspaceUpdates(sessionId, wrapper).also {
            registeredListeners.remove(listener)
        }
    }

    /**
     *  Closes this session, all subsequent calls will result in an [IllegalStateException].
     */
    suspend fun close() {
        throwIfClosed()
        isClosed.set(true)
        destroySmartspaceSession(sessionId)
    }

    private fun throwIfClosed() {
        if(isClosed.get()){
            throw IllegalStateException("Session has already been closed")
        }
    }

    interface OnTargetsAvailableListener {
        /**
         *  Called when new or updated Targets are available
         */
        fun onTargetsAvailable(targets: List<SmartspaceTarget>)
    }

    private class CallbackWrapper(
        private val executor: Executor,
        private val listener: OnTargetsAvailableListener
    ): ISmartspaceCallback.Stub() {
        override fun onResult(result: ParceledListSlice<*>) {
            val identity = Binder.clearCallingIdentity()
            try {
                val list = result.list as List<Bundle>
                executor.execute { listener.onTargetsAvailable(list.map { SmartspaceTarget(it) }) }
            }finally {
                Binder.restoreCallingIdentity(identity)
            }
        }
    }

}