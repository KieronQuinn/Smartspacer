/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kieronquinn.app.smartspacer.components.smartspace

import android.app.smartspace.ISmartspaceCallback
import android.app.smartspace.SmartspaceConfig
import android.app.smartspace.SmartspaceSessionId
import android.app.smartspace.SmartspaceTarget
import android.app.smartspace.SmartspaceTargetEvent
import android.content.Context
import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.RemoteException
import android.service.smartspace.ISmartspaceService
import android.util.ArrayMap
import android.util.Log
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.utils.extensions.getUser
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Client API to share information about the Smartspace UI state and execute query.
 *
 *
 *
 * Usage: <pre> `class MyActivity {
 * private SmartspaceSession mSmartspaceSession;
 *
 * void onCreate() {
 * mSmartspaceSession = mSmartspaceManager.createSmartspaceSession(smartspaceConfig)
 * mSmartspaceSession.registerSmartspaceUpdates(...)
 * }
 *
 * void onStart() {
 * mSmartspaceSession.requestSmartspaceUpdate()
 * }
 *
 * void onTouch(...) OR
 * void onStateTransitionStarted(...) OR
 * void onResume(...) OR
 * void onStop(...) {
 * mSmartspaceSession.notifyEvent(event);
 * }
 *
 * void onDestroy() {
 * mSmartspaceSession.unregisterPredictionUpdates()
 * mSmartspaceSession.close();
 * }
 *
 * }</pre>
 *
` *
</pre> */
class SmartspaceSession(
    private val mInterface: ISmartspaceService,
    context: Context,
    private val smartspaceConfig: SmartspaceConfig
): AutoCloseable {
    private val mIsClosed = AtomicBoolean(false)

    private val mSessionId: SmartspaceSessionId = SmartspaceSessionId(
        context.packageName + ":" + UUID.randomUUID().toString(), context.getUser()
    )
    private val mRegisteredCallbacks = ArrayMap<OnTargetsAvailableListener, CallbackWrapper>()

    /**
     * Creates a new Smartspace ui client.
     *
     *
     * The caller should call [SmartspaceSession.destroy] to dispose the client once it
     * no longer used.
     *
     * @param context          the [Context] of the user of this [SmartspaceSession].
     * @param smartspaceConfig the Smartspace context.
     */
    // b/177858121 Create weak reference child objects to not leak context.
    init {
        try {
            mInterface.onCreateSmartspaceSession(smartspaceConfig, mSessionId)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to create Smartspace session", e)
        }
    }

    /**
     * Notifies the Smartspace service of a Smartspace target event.
     *
     * @param event The [SmartspaceTargetEvent] that represents the Smartspace target event.
     */
    fun notifySmartspaceEvent(event: SmartspaceTargetEvent) {
        check(!mIsClosed.get()) { "This client has already been destroyed." }
        try {
            mInterface.notifySmartspaceEvent(mSessionId, event)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to notify event", e)
        }
    }

    /**
     * Requests the smartspace service for an update.
     */
    fun requestSmartspaceUpdate() {
        check(!mIsClosed.get()) { "This client has already been destroyed." }
        try {
            mInterface.requestSmartspaceUpdate(mSessionId)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to request update.", e)
        }
    }

    /**
     * Requests the smartspace service provide continuous updates of smartspace cards via the
     * provided callback, until the given callback is unregistered.
     *
     * @param listenerExecutor The listener executor to use when firing the listener.
     * @param listener         The listener to be called when updates of Smartspace targets are
     * available.
     */
    fun addOnTargetsAvailableListener(
        listenerExecutor: Executor,
        listener: OnTargetsAvailableListener
    ) {
        check(!mIsClosed.get()) { "This client has already been destroyed." }

        if (mRegisteredCallbacks.containsKey(listener)) {
            // Skip if this callback is already registered
            return
        }
        try {
            val callbackWrapper = CallbackWrapper(
                listenerExecutor, smartspaceConfig
            ) { targets: List<SmartspaceTarget?> ->
                listener.onTargetsAvailable(
                    targets
                )
            }
            mRegisteredCallbacks[listener] = callbackWrapper
            mInterface.registerSmartspaceUpdates(mSessionId, callbackWrapper)
            mInterface.requestSmartspaceUpdate(mSessionId)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register for smartspace updates", e)
        }
    }

    /**
     * Requests the smartspace service to stop providing continuous updates to the provided
     * callback until the callback is re-registered.
     *
     * @param listener The callback to be unregistered.
     * @see {@link SmartspaceSession.addOnTargetsAvailableListener
     */
    fun removeOnTargetsAvailableListener(listener: OnTargetsAvailableListener) {
        check(!mIsClosed.get()) { "This client has already been destroyed." }

        if (!mRegisteredCallbacks.containsKey(listener)) {
            // Skip if this callback was never registered
            return
        }
        try {
            val callbackWrapper = mRegisteredCallbacks.remove(listener)
            mInterface.unregisterSmartspaceUpdates(mSessionId, callbackWrapper)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to unregister for smartspace updates", e)
        }
    }

    /**
     * Destroys the client and unregisters the callback. Any method on this class after this call
     * will throw [IllegalStateException].
     */
    private fun destroy() {
        if (!mIsClosed.getAndSet(true)) {
            // Do destroy;
            try {
                mInterface.onDestroySmartspaceSession(mSessionId)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to notify Smartspace target event", e)
            }
        } else {
            throw IllegalStateException("This client has already been destroyed.")
        }
    }

    override fun close() {
        try {
            destroy()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    /**
     * Listener to receive smartspace targets from the service.
     */
    interface OnTargetsAvailableListener {
        /**
         * Called when a new set of smartspace targets are available.
         *
         * @param targets Ranked list of smartspace targets.
         */
        fun onTargetsAvailable(targets: List<SmartspaceTarget?>)
    }

    internal class CallbackWrapper(
        private val mExecutor: Executor,
        private val config: SmartspaceConfig,
        private val mCallback: Consumer<List<SmartspaceTarget?>>
    ) :
        ISmartspaceCallback.Stub() {
        override fun onResult(result: ParceledListSlice<*>) {
            val identity = Binder.clearCallingIdentity()
            try {
                if (DEBUG) {
                    Log.d(TAG, "CallbackWrapper.onResult ${config.uiSurface} result=" + result.list)
                }
                mExecutor.execute { mCallback.accept(result.list as List<SmartspaceTarget?>) }
            } finally {
                Binder.restoreCallingIdentity(identity)
            }
        }
    }
    companion object {

        private val TAG: String = SmartspaceSession::class.java.simpleName
        private val DEBUG = BuildConfig.DEBUG
    }
}