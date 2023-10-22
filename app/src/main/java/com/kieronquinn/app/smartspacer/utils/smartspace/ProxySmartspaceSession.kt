package com.kieronquinn.app.smartspacer.utils.smartspace

import android.app.smartspace.SmartspaceSession
import android.app.smartspace.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.ISmartspaceOnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.ISmartspaceSession
import java.util.UUID
import java.util.concurrent.Executors

class ProxySmartspaceSession(
    private val smartspaceSession: SmartspaceSession
): ISmartspaceSession.Stub() {

    private var listeners = HashMap<String, SmartspaceSession.OnTargetsAvailableListener>()
    private val executor = Executors.newSingleThreadExecutor()

    override fun notifySmartspaceEvent(event: SmartspaceTargetEvent) {
        smartspaceSession.notifySmartspaceEvent(event)
    }

    override fun requestSmartspaceUpdate() {
        smartspaceSession.requestSmartspaceUpdate()
    }

    override fun addOnTargetsAvailableListener(
        listener: ISmartspaceOnTargetsAvailableListener
    ): String {
        val proxy = ProxySmartspaceOnTargetsAvailableListener(listener)
        val id = UUID.randomUUID().toString()
        smartspaceSession.addOnTargetsAvailableListener(executor, proxy)
        listeners[id] = proxy
        return id
    }

    override fun removeOnTargetsAvailableListener(id: String) {
        val listener = listeners.remove(id) ?: return
        smartspaceSession.removeOnTargetsAvailableListener(listener)
    }

    override fun close() {
        smartspaceSession.close()
    }

}