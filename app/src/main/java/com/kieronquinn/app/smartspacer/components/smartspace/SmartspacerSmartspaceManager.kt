package com.kieronquinn.app.smartspacer.components.smartspace

import android.app.IServiceConnection
import android.app.smartspace.SmartspaceConfig
import android.app.smartspace.SmartspaceSessionId
import android.app.smartspace.SmartspaceTarget
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.service.smartspace.ISmartspaceService
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.components.smartspace.SmartspaceSession.OnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultSmartspaceComponent
import com.kieronquinn.app.smartspacer.utils.extensions.getIApplicationThread
import com.kieronquinn.app.smartspacer.utils.extensions.getMainThreadHandler
import com.kieronquinn.app.smartspacer.utils.extensions.getServiceDispatcher
import com.kieronquinn.app.smartspacer.utils.extensions.suspendCoroutineWithTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 *  Replacement for [android.app.smartspace.SmartspaceManager] that calls directly to the system
 *  service, if it exists.
 */
class SmartspacerSmartspaceManager(private val context: Context): KoinComponent {

    companion object {
        private const val BIND_TIMEOUT = 2500L
    }

    private val shizuku by inject<ShizukuServiceRepository>()
    private val systemSmartspaceRepository by inject<SystemSmartspaceRepository>()

    private val serviceIntent = context.getDefaultSmartspaceComponent()?.let {
        Intent("android.service.smartspace.SmartspaceService").apply {
            component = it
        }
    }

    private val bindLock = Mutex()
    private var serviceConnection: IServiceConnection? = null
    private var service: ISmartspaceService? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val applicationThread by lazy {
        context.getIApplicationThread().asBinder()
    }
    private val handler by lazy {
        context.getMainThreadHandler()
    }

    private val deathRecipient = IBinder.DeathRecipient {
        serviceConnection = null
        service = null
        systemSmartspaceRepository.onAsiStopped()
    }

    val isAvailable = serviceIntent != null

    suspend fun createSmartspaceSessions(
        onTargetsAvailable: (surface: UiSurface, targets: List<SmartspaceTarget>) -> Unit
    ) {
        shizuku.runWithService {
            it.runWithServiceLocked {
                UiSurface.entries.forEach { surface ->
                    val config = SmartspaceConfig.Builder(context, surface.surface)
                        .setSmartspaceTargetCount(5)
                        .build()
                    SmartspaceSession(this, context, config).apply {
                        addOnTargetsAvailableListener(
                            executor, createCallback(surface, onTargetsAvailable)
                        )
                    }
                }
            }
        }
    }

    private fun createCallback(
        surface: UiSurface,
        callback: (surface: UiSurface, targets: List<SmartspaceTarget>) -> Unit
    ): OnTargetsAvailableListener {
        return object: OnTargetsAvailableListener {
            override fun onTargetsAvailable(targets: List<SmartspaceTarget?>) {
                callback(surface, targets.filterNotNull())
            }
        }
    }

    suspend fun destroySmartspaceSession(sessionId: SmartspaceSessionId) {
        shizuku.runWithService {
            it.runWithServiceLocked {
                onDestroySmartspaceSession(sessionId)
            }
        }
    }

    private suspend fun <T> ISmartspacerShizukuService.runWithServiceLocked(
        block: ISmartspaceService.() -> T
    ) = bindLock.withLock {
        runWithService(block)
    }

    private suspend fun <T> ISmartspacerShizukuService.runWithService(
        block: ISmartspaceService.() -> T
    ): T? = suspendCoroutineWithTimeout(BIND_TIMEOUT) { resume ->
        var hasResumed = false
        service?.let {
            try {
                hasResumed = true
                resume.resume(block(it))
                return@suspendCoroutineWithTimeout
            }catch (e: DeadObjectException) {
                //Service died, reconnect
            }
        }
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                service.linkToDeath(deathRecipient, 0)
                val connection = ISmartspaceService.Stub.asInterface(service)
                this@SmartspacerSmartspaceManager.service = connection
                if(!hasResumed) {
                    hasResumed = true
                    resume.resume(block(connection))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                serviceConnection = null
            }
        }
        val dispatcher = context.getServiceDispatcher(serviceConnection, handler, 0)
        this@SmartspacerSmartspaceManager.serviceConnection = dispatcher
        bindService(dispatcher.asBinder(), applicationThread, serviceIntent)
    }

}