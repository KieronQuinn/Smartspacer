package com.kieronquinn.app.smartspacer.sdk.client.helper

import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import com.kieronquinn.app.smartspacer.sdk.client.SmartspaceSession
import com.kieronquinn.app.smartspacer.sdk.client.SmartspaceSession.OnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent.Companion.EVENT_TARGET_BLOCK
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent.Companion.EVENT_TARGET_DISMISS
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent.Companion.EVENT_TARGET_INTERACTION
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent.Companion.EVENT_UI_SURFACE_HIDDEN
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent.Companion.EVENT_UI_SURFACE_SHOWN
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SmartspacerHelper(
    private val client: SmartspacerClient,
    private val config: SmartspaceConfig
) {

    private val defaultExecutor = Executors.newSingleThreadExecutor()
    private val sessionLock = Mutex()
    private val listeners = ArrayList<Pair<Executor, OnTargetsAvailableListener>>()
    private var scope = MainScope()
    private var errorListener: ErrorListener? = null
    private var session: SmartspaceSession? = null
    private var isResumed = false

    /**
     *  Convenience [Flow] for collecting [SmartspaceTarget]s
     */
    val targets = callbackFlow {
        val listener = object: OnTargetsAvailableListener {
            override fun onTargetsAvailable(targets: List<SmartspaceTarget>) {
                trySend(targets)
            }
        }
        addTargetsAvailableListener(listener = listener)
        awaitClose {
            removeTargetsAvailableListener(listener)
        }
    }.map {
        it.assertAtLeastDateTarget()
    }

    /**
     *  Handles lifecycle [Lifecycle.Event.ON_CREATE] event, call from your activity or fragment's
     *  `onCreate` method or the ViewModel's init block if you are using a ViewModel.
     */
    fun onCreate() {
        scope.cancel()
        scope = MainScope()
    }

    /**
     *  Closes any open sessions, and the client. Call in your activity or fragment's `onDestroy`
     *  method, or the ViewModel's `onCleared` if you are using a ViewModel.
     */
    fun onDestroy() {
        scope.launch {
            closeSession()
            scope.cancel()
        }
    }

    /**
     *  Calls the [EVENT_UI_SURFACE_SHOWN] event, also handling the [Lifecycle.Event.ON_RESUME]
     *  lifecycle event. Invoke in your fragment or activity's `onResume` method  (via a ViewModel
     *  if available)
     */
    fun onResume() {
        isResumed = true
        runWithSession {
            it.notifySmartspaceEvent(createEvent(
                event = EVENT_UI_SURFACE_SHOWN
            ))
            it.requestSmartspaceUpdate()
        }
    }

    /**
     *  Calls the [EVENT_UI_SURFACE_HIDDEN] event, also handling the [Lifecycle.Event.ON_PAUSE]
     *  lifecycle event. Invoke in your fragment or activity's `onPause` method  (via a ViewModel if
     *  available).
     */
    fun onPause() {
        isResumed = false
        runWithSession {
            it.notifySmartspaceEvent(createEvent(
                event = EVENT_UI_SURFACE_HIDDEN
            ))
        }
    }

    /**
     *  Call the [EVENT_TARGET_INTERACTION] event for a given [target] and [actionId]
     */
    fun onTargetInteraction(target: SmartspaceTarget, actionId: String? = null) {
        runWithSession {
            it.notifySmartspaceEvent(createEvent(
                target = target,
                actionId = actionId,
                event = EVENT_TARGET_INTERACTION
            ))
        }
    }

    /**
     *  Call the [EVENT_TARGET_DISMISS] event for a given [target]
     */
    fun onTargetDismiss(target: SmartspaceTarget) {
        runWithSession {
            it.notifySmartspaceEvent(createEvent(
                target = target,
                event = EVENT_TARGET_DISMISS
            ))
        }
    }

    /**
     *  Call the [EVENT_TARGET_BLOCK] event for a given [target] and [actionId]. This is not
     *  currently used by Smartspacer or any known system launchers.
     */
    fun onTargetBlocked(target: SmartspaceTarget, actionId: String? = null) {
        runWithSession {
            it.notifySmartspaceEvent(createEvent(
                target = target,
                actionId = actionId,
                event = EVENT_TARGET_BLOCK
            ))
        }
    }

    /**
     *  Adds a target listener to the session.
     *
     *  **Note:** Listeners survive session recreation automatically when using [SmartspacerHelper],
     *  you do not need to re-register listeners after errors.
     */
    fun addTargetsAvailableListener(
        executor: Executor = defaultExecutor,
        listener: OnTargetsAvailableListener
    ) {
        if(!listeners.any { it.second == listener }){
            listeners.add(Pair(executor, listener))
        }
        runWithSession {
            it.addTargetsAvailableListener(executor, listener)
        }
    }

    /**
     *  Remove a given listener
     */
    fun removeTargetsAvailableListener(listener: OnTargetsAvailableListener) {
        val wasRemoved = listeners.removeIf { it.second == listener }
        runWithSession {
            it.removeTargetsAvailableListener(listener)
        }
    }

    /**
     *  Set a listener to be invoked if the Smartspace Session fails to be created. This happens
     *  when the service fails to connect. This does not necessarily only happen immediately,
     *  as the user uninstalling or disabling Smartspacer would invoke this listener on the next
     *  attempted call.
     */
    fun setErrorListener(listener: ErrorListener?) {
        errorListener = listener
    }

    /**
     *  Closes out the current session, if it exists. This means any further calls will get a clean
     *  session.
     */
    fun closeSession() {
        scope.launch {
            sessionLock.withLock {
                session?.close()
                session = null
            }
        }
    }

    private fun runWithSession(block: suspend (SmartspaceSession) -> Boolean) {
        scope.launch {
            sessionLock.withLock {
                runWithSessionLocked(block)
            }
        }
    }

    private suspend fun runWithSessionLocked(block: suspend (SmartspaceSession) -> Boolean) {
        val session = session ?: createSession() ?: run {
            errorListener?.onError()
            return
        }
        if(!block(session)){
            //Session has died, clear instance and re-call (this will error if it fails to recreate)
            this.session = null
            runWithSessionLocked(block)
        }
    }

    private suspend fun createSession(): SmartspaceSession? {
        val session = client.createSmartspaceSession(config)
        this.session = session
        if(isResumed){
            session?.notifySmartspaceEvent(createEvent(event = EVENT_UI_SURFACE_SHOWN))
        }
        listeners.forEach {
            session?.addTargetsAvailableListener(it.first, it.second)
        }
        return session
    }

    private fun createEvent(
        target: SmartspaceTarget? = null,
        actionId: String? = null,
        event: Int
    ): SmartspaceTargetEvent {
        return SmartspaceTargetEvent(target, actionId, event)
    }

    private fun List<SmartspaceTarget>.assertAtLeastDateTarget(): List<SmartspaceTarget> {
        if(this.isNotEmpty()) return this
        return listOf(
            SmartspaceTarget(
                smartspaceTargetId = UUID.randomUUID().toString(),
                featureType = SmartspaceTarget.FEATURE_WEATHER,
                componentName = ComponentName("date", "empty")
            )
        )
    }

    interface ErrorListener {
        /**
         *  Called when [SmartspaceSession] fails to connect. This is usually caused by the app
         *  being uninstalled, disabled or an internal error.
         *  You can use this to display an error to the user, or disable Smartspacer functionality.
         */
        fun onError()
    }

}