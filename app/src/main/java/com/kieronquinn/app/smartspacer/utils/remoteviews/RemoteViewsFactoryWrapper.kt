package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.RemoteException
import android.widget.RemoteViews
import android.widget.RemoteViewsAdapter.EXTRA_REMOTEADAPTER_APPWIDGET_ID
import com.android.internal.widget.IRemoteViewsFactory
import com.kieronquinn.app.smartspacer.utils.extensions.bindRemoteViewsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 *  Connects to a remote [IRemoteViewsFactory] for a given [intent] service, which is then passed on
 *  to plugins. This allows plugins to retrieve data, before the service is then disconnected again
 *  after 10 seconds.
 */
class RemoteViewsFactoryWrapper(
    private val context: Context,
    private val intent: Intent,
    val viewId: Int,
    private val clear: () -> Unit
): ServiceConnection {

    companion object {
        private const val DELAY_DISCONNECT = 10_000L // 10 seconds
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private var listener: Listener? = null
    private var service: IRemoteViewsFactory? = null
    private val scope = MainScope()
    private var disconnectJob: Job? = null
    private var bindRequested = false

    private val workerThread = HandlerThread("RemoteViewsFactoryThread").apply {
        start()
    }

    private val handler = Handler(workerThread.looper)

    suspend fun awaitConnected(): RemoteAdapter = callbackFlow {
        val listener = object: Listener {
            override fun onConnected(factory: IRemoteViewsFactory) {
                this@RemoteViewsFactoryWrapper.listener = null
                trySend(RemoteAdapter(context, this@RemoteViewsFactoryWrapper))
            }
        }
        service?.let {
            trySend(RemoteAdapter(context, this@RemoteViewsFactoryWrapper))
        } ?: run {
            this@RemoteViewsFactoryWrapper.listener = listener
            if(!bindRequested){
                connectToService()
            }
        }
        awaitClose {
            //No-op
        }
    }.first()

    fun disconnect(clear: Boolean = false) {
        context.unbindService(this@RemoteViewsFactoryWrapper)
        onDisconnected()
        if(clear){
            workerThread.quit()
        }
    }

    private fun connectToService() {
        val appWidgetId = intent.getIntExtra(EXTRA_REMOTEADAPTER_APPWIDGET_ID, -1)
        if(appWidgetId == -1) return
        disconnectJob?.cancel()
        bindRequested = appWidgetManager.bindRemoteViewsService(
            context, appWidgetId, intent, this, handler
        )
    }

    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
        disconnectJob?.cancel()
        val service = IRemoteViewsFactory.Stub.asInterface(binder)
        this.service = service
        listener?.onConnected(service)
        disconnectJob?.cancel()
        enqueueDisconnect()
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        onDisconnected()
    }

    fun getViewAt(index: Int): RemoteViews? {
        return runRemote {
            it.getViewAt(index)
        }
    }

    fun getCount(): Int {
        return runRemote {
            it.count
        } ?: 0
    }

    private fun onDisconnected() {
        clear()
        service = null
        disconnectJob?.cancel()
        scope.cancel()
    }

    private fun enqueueDisconnect() = scope.launch {
        delay(DELAY_DISCONNECT)
        disconnect()
    }.also {
        disconnectJob = it
    }

    private fun <T> runRemote(block: (IRemoteViewsFactory) -> T?): T? {
        return try {
            service?.let { block(it) }
        }catch (e: RemoteException) {
            //Service has died
            onDisconnected()
            null
        }
    }

    interface Listener {
        fun onConnected(factory: IRemoteViewsFactory)
    }
}