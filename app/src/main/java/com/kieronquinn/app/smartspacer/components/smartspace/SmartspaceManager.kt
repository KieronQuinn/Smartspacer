package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import android.content.IntentSender
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.sdk.client.ISmartspaceCallback
import com.kieronquinn.app.smartspacer.sdk.client.ISmartspaceManager
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice
import com.kieronquinn.app.smartspacer.ui.activities.permission.client.SmartspacerClientPermissionActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspaceManager(private val context: Context): ISmartspaceManager.Stub(), KoinComponent {

    private val sessionsLock = Mutex()
    private val sessions = HashMap<SmartspaceSessionId, ClientSmartspacerSession>()
    private val callbacksLock = Mutex()
    private val callbacks = ArrayList<Pair<SmartspaceSessionId, ISmartspaceCallback>>()
    private val databaseRepository by inject<DatabaseRepository>()
    private val scope = MainScope()

    override fun ping() = true

    override fun createSmartspaceSession(config: Bundle, sessionId: Bundle, token: IBinder) {
        val smartspaceConfig = SmartspaceConfig(config)
        val calling = getCallingPackage()
        if(smartspaceConfig.packageName != calling){
            throw SecurityException(
                "Config package ${smartspaceConfig.packageName} does not match calling package $calling"
            )
        }
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        if(sessions.containsKey(smartspaceSessionId)){
            destroySmartspaceSession(sessionId)
        }
        synchronized(sessionsLock) {
            sessions[smartspaceSessionId] = ClientSmartspacerSession(
                context,
                smartspaceConfig,
                smartspaceSessionId,
                ::onSmartspaceUpdate
            )
        }
        token.linkToDeath({
            destroySmartspaceSession(sessionId)
        }, 0)
    }

    override fun notifySmartspaceEvent(sessionId: Bundle, event: Bundle): Boolean {
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        val smartspaceEvent = SmartspaceTargetEvent(event)
        val session = getSession(smartspaceSessionId) ?: return false
        session.notifySmartspaceEvent(smartspaceEvent)
        return true
    }

    override fun requestSmartspaceUpdate(sessionId: Bundle): Boolean {
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        val session = getSession(smartspaceSessionId) ?: return false
        session.requestSmartspaceUpdate()
        return true
    }

    override fun registerSmartspaceUpdates(sessionId: Bundle, callback: ISmartspaceCallback): Boolean {
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        val session = getSession(smartspaceSessionId) ?: return false
        unregisterSmartspaceUpdates(sessionId, callback)
        callbacks.add(Pair(smartspaceSessionId, callback))
        session.lastTargets?.let {
            onSmartspaceUpdate(smartspaceSessionId, it)
        }
        return true
    }

    override fun unregisterSmartspaceUpdates(
        sessionId: Bundle,
        callback: ISmartspaceCallback
    ): Boolean {
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        getSession(smartspaceSessionId) ?: return false
        synchronized(callbacksLock) {
            callbacks.removeIf { it.first == smartspaceSessionId && it.second == callback }
        }
        return true
    }

    override fun destroySmartspaceSession(sessionId: Bundle) {
        val smartspaceSessionId = SmartspaceSessionId(sessionId)
        val session = getSession(smartspaceSessionId) ?: return
        synchronized(sessionsLock) {
            session.onPause()
            session.onDestroy()
            sessions.remove(smartspaceSessionId)
            synchronized(callbacksLock) {
                callbacks.removeIf { it.first == smartspaceSessionId }
            }
        }
    }

    override fun createPermissionRequestIntentSender(): IntentSender? {
        val calling = getCallingPackage() ?: return null
        val grant = getGrantForPackage(calling) ?: Grant(calling)
        val pendingIntent = SmartspacerClientPermissionActivity.createPendingIntent(context, grant)
        return pendingIntent.intentSender
    }

    override fun checkCallingPermission(): Boolean {
        val calling = getCallingPackage() ?: return false
        return getGrantForPackage(calling)?.smartspace ?: false
    }

    override fun onDestroy() {
        val callingPackage = getCallingPackage() ?: return
        scope.launch {
            sessionsLock.withLock {
                val ownedSessions = sessions.filter { it.value.owner == callingPackage }
                ownedSessions.forEach { destroySmartspaceSession(it.key.toBundle()) }
            }
        }
    }

    fun onServiceDestroy() {
        scope.launch {
            sessionsLock.withLock {
                sessions.forEach { destroySmartspaceSession(it.key.toBundle()) }
            }
        }
        scope.cancel()
    }

    private fun getGrantForPackage(packageName: String) = runBlocking {
        databaseRepository.getGrantForPackage(packageName)
    }

    private fun getCallingPackage(): String? {
        return context.packageManager.getNameForUid(Binder.getCallingUid())
    }

    private fun onSmartspaceUpdate(sessionId: SmartspaceSessionId, targets: List<SmartspaceTarget>) {
        synchronized(callbacksLock) {
            callbacks.filter { it.first == sessionId }.listIterator().forEach {
                try {
                    it.second.onResult(
                        ParceledListSlice(
                            targets.map { target -> target.toBundle() })
                    )
                } catch (e: RemoteException) {
                    //Callback died, remove so we don't call it again
                    callbacks.remove(it)
                }
            }
        }
    }

    private fun getSession(sessionId: SmartspaceSessionId) = synchronized(sessionsLock) {
        sessions[sessionId]
    }

}