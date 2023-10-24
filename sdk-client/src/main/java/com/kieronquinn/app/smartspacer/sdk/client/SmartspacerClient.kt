package com.kieronquinn.app.smartspacer.sdk.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SmartspacerClient constructor(context: Context) {

    companion object {
        private const val ACTION_MANAGER = "com.kieronquinn.app.smartspacer.MANAGER"
        private const val TAG = "SmartspacerClient"
        private val instanceLock = Object()

        @JvmStatic
        private var INSTANCE: SmartspacerClient? = null

        /**
         *  Gets the current instance, or creates a new instance of Smartspacer Client
         */
        fun getInstance(context: Context): SmartspacerClient {
            return synchronized(instanceLock) {
                INSTANCE ?: SmartspacerClient(context).also {
                    INSTANCE = it
                }
            }
        }

        /**
         *  If there is an instance of the client, closes it and then clears the instance.
         */
        fun close() {
            synchronized(instanceLock) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }

    private val applicationContext = context.applicationContext
    private val serviceLock = Mutex()
    private var serviceConnection: ServiceConnection? = null
    private var service: ISmartspaceManager? = null
    private val token = Binder()

    /**
     *  Creates a new [SmartspaceSession] for a given [config].
     *
     *  Note: a `null` response indicates failure to connect to Smartspacer, likely due to it not
     *  being installed.
     */
    suspend fun createSmartspaceSession(config: SmartspaceConfig): SmartspaceSession? {
        val sessionId = SmartspaceSessionId(
            "${applicationContext.packageName}:${UUID.randomUUID()}",
            Process.myUserHandle()
        )
        if(createSmartspaceSession(config, sessionId, token) == null) return null
        return SmartspaceSession(
            sessionId,
            ::notifySmartspaceEvent,
            ::requestSmartspaceUpdate,
            ::registerSmartspaceUpdates,
            ::unregisterSmartspaceUpdates,
            ::destroySmartspaceSession
        )
    }

    /**
     *  Disconnects the service, if it is still connected.
     */
    fun close() {
        serviceConnection?.let {
            try {
                applicationContext.unbindService(it)
            }catch (e: IllegalArgumentException){
                //Already unbound
            }
        }
    }

    /**
     *  Gets an [IntentSender] which can be used to start the permission prompt, without requiring
     *  the user tap on the permission request Target
     */
    suspend fun createPermissionRequestIntentSender(): IntentSender? {
        return runWithService {
            it.createPermissionRequestIntentSender()
        }
    }

    /**
     *  Checks whether the calling app has permission to access Smartspacer. Returns `null` if
     *  the service is not available.
     */
    suspend fun checkCallingPermission(): Boolean? {
        return runWithService {
            it.checkCallingPermission()
        }
    }

    private suspend fun createSmartspaceSession(
        config: SmartspaceConfig,
        sessionId: SmartspaceSessionId,
        token: IBinder
    ) = runWithService {
        it.createSmartspaceSession(config.toBundle(), sessionId.toBundle(), token)
    }

    private suspend fun notifySmartspaceEvent(
        sessionId: SmartspaceSessionId,
        event: SmartspaceTargetEvent
    ) = runWithService {
        it.notifySmartspaceEvent(sessionId.toBundle(), event.toBundle())
    }.logIfFailed("notifySmartspaceEvent")

    private suspend fun requestSmartspaceUpdate(
        sessionId: SmartspaceSessionId
    ) = runWithService {
        it.requestSmartspaceUpdate(sessionId.toBundle())
    }.logIfFailed("requestSmartspaceUpdate")

    private suspend fun registerSmartspaceUpdates(
        sessionId: SmartspaceSessionId,
        callback: ISmartspaceCallback
    ) = runWithService {
        it.registerSmartspaceUpdates(sessionId.toBundle(), callback)
    }.logIfFailed("registerSmartspaceUpdates")

    private suspend fun unregisterSmartspaceUpdates(
        sessionId: SmartspaceSessionId,
        callback: ISmartspaceCallback
    ) = runWithService {
        it.unregisterSmartspaceUpdates(sessionId.toBundle(), callback)
    }.logIfFailed("unregisterSmartspaceUpdates")

    private suspend fun destroySmartspaceSession(
        sessionId: SmartspaceSessionId
    ) = runWithService {
        it.destroySmartspaceSession(sessionId.toBundle())
    }

    private suspend fun <T> runWithService(
        block: suspend (ISmartspaceManager) -> T?
    ): T? = serviceLock.withLock {
        try {
            getService()?.let { block(it) }
        }catch (e: RemoteException){
            Log.e(TAG, "Error running remote call", e)
            null
        }
    }

    private suspend fun getService(): ISmartspaceManager? = withContext(Dispatchers.IO) {
        suspendCoroutine {
            var hasResumed = false
            val serviceConnection = object: ServiceConnection {
                override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                    serviceConnection = this
                    val service = ISmartspaceManager.Stub.asInterface(binder)
                    this@SmartspacerClient.service = service
                    if(!hasResumed){
                        hasResumed = true
                        it.resume(service)
                    }
                }

                override fun onServiceDisconnected(component: ComponentName) {
                    serviceConnection = null
                    service = null
                }
            }
            val success = applicationContext.bindService(
                getServiceIntent(), serviceConnection, Context.BIND_AUTO_CREATE
            )
            if(!success){
                hasResumed = true
                it.resume(null)
            }
        }
    }

    private fun getServiceIntent(): Intent {
        return Intent(ACTION_MANAGER).apply {
            `package` = SmartspacerConstants.SMARTSPACER_PACKAGE_NAME
        }
    }

    private fun Boolean?.logIfFailed(methodName: String): Boolean {
        if(this == null){
            Log.e(TAG, "Failed to perform $methodName, service connect error")
        }
        return this ?: false
    }

}