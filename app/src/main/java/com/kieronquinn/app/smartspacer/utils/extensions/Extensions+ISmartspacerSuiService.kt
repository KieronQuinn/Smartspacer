package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import androidx.core.content.ContextCompat
import com.kieronquinn.app.smartspacer.IRunningAppObserver
import com.kieronquinn.app.smartspacer.ISmartspacerSuiService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/**
 *  Sends a broadcast of [intent] as root.
 */
@SuppressLint("NewApi") //Exists, just hidden
fun ISmartspacerSuiService.sendBroadcast(context: Context, intent: Intent) {
    val applicationThread = context.getIApplicationThread()
    val attributionTag = ContextCompat.getAttributionTag(context)
    return sendPrivilegedBroadcast(applicationThread.asBinder(), attributionTag, intent)
}

fun ISmartspacerSuiService.runningApp() = callbackFlow {
    val observer = object: IRunningAppObserver.Stub() {
        override fun onRunningAppChanged(packageName: String) {
            trySend(packageName)
        }
    }
    setProcessObserver(observer)
    awaitClose {
        try {
            setProcessObserver(null)
        }catch (e: RemoteException){
            //Process died
        }
    }
}