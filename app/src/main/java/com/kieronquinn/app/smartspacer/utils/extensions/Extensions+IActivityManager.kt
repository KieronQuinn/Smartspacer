package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.IProcessObserver
import android.app.IServiceConnection
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import android.util.Log
import androidx.core.os.BuildCompat.isAtLeastT
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun IActivityManager.processDied() = callbackFlow<Int> {
    val observer = object: IProcessObserver.Stub() {
        override fun onProcessDied(pid: Int, uid: Int) {
            trySend(uid)
        }

        override fun onForegroundActivitiesChanged(
            pid: Int,
            uid: Int,
            foregroundActivities: Boolean
        ) {
            //No-op
        }

        override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {
            //No-op
        }

        override fun onProcessStarted(
            pid: Int,
            processUid: Int,
            packageUid: Int,
            packageName: String?,
            processName: String?
        ) {
            //No-op
        }
    }
    registerProcessObserver(observer)
    awaitClose {
        unregisterProcessObserver(observer)
    }
}

/**
 *  Handles differences between calls on Android 11 and 12+
 */
@SuppressLint("UnsafeOptInUsageError")
fun IActivityManager.broadcastIntentWithFeatureCompat(
    thread: IApplicationThread,
    attributionTag: String?,
    intent: Intent,
    intentType: String?,
    identifier: Int
) {
    val options = arrayOf(
        IActivityManager::broadcastIntentOptionA,
        IActivityManager::broadcastIntentOptionB,
        IActivityManager::broadcastIntentOptionC
    )
    options.firstOrNull {
        it.invoke(this, thread, attributionTag, intent, intentType, identifier) == null
    } ?: throw NoSuchMethodError("Failed to find broadcastIntentWithFeature method")
}

private fun IActivityManager.broadcastIntentOptionA(
    thread: IApplicationThread,
    attributionTag: String?,
    intent: Intent,
    intentType: String?,
    identifier: Int
): Throwable? {
    return try {
        broadcastIntentWithFeature(
            thread,
            attributionTag,
            intent,
            intentType,
            null,
            Activity.RESULT_OK,
            null,
            null,
            null,
            null,
            null,
            -1,
            null,
            false,
            false,
            identifier
        )
        null
    }catch (e: NoSuchMethodError){
        e
    }
}

private fun IActivityManager.broadcastIntentOptionB(
    thread: IApplicationThread,
    attributionTag: String?,
    intent: Intent,
    intentType: String?,
    identifier: Int
): Throwable? {
    return try {
        broadcastIntentWithFeature(
            thread,
            attributionTag,
            intent,
            intentType,
            null,
            Activity.RESULT_OK,
            null,
            null,
            null,
            null,
            -1,
            null,
            false,
            false,
            identifier
        )
        null
    }catch (e: NoSuchMethodError){
        e
    }
}

private fun IActivityManager.broadcastIntentOptionC(
    thread: IApplicationThread,
    attributionTag: String?,
    intent: Intent,
    intentType: String?,
    identifier: Int
): Throwable? {
    return try {
        broadcastIntentWithFeature(
            thread,
            attributionTag,
            intent,
            intentType,
            null,
            Activity.RESULT_OK,
            null,
            null,
            null,
            -1,
            null,
            false,
            false,
            identifier
        )
        null
    }catch (e: NoSuchMethodError){
        e
    }
}

fun IActivityManager.bindServiceInstanceCompat(
    context: Context,
    serviceConnection: IServiceConnection,
    thread: IApplicationThread?,
    token: IBinder?,
    intent: Intent,
    flags: Int,
    packageNameOverride: String? = null
): Int {
    try {
        val packageName = packageNameOverride
            ?: Context::class.java.getMethod("getOpPackageName").invoke(context) as String
        val userHandle = Context::class.java.getMethod("getUser").invoke(context) as UserHandle
        val identifier =
            UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as Int
        Intent::class.java.getMethod("prepareToLeaveProcess", Context::class.java)
            .invoke(intent, context)
        return bindServiceInstanceCompat(
            thread,
            token,
            intent,
            null,
            serviceConnection,
            flags,
            null,
            packageName,
            identifier
        )
    } catch (e: Exception) {
        Log.e("ServiceBind", "Error binding service", e)
        return 0
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun IActivityManager.bindServiceInstanceCompat(
    caller: IApplicationThread?,
    token: IBinder?,
    service: Intent?,
    resolvedType: String?,
    connection: IServiceConnection?,
    flags: Int,
    instanceName: String?,
    callingPackage: String?,
    userId: Int
): Int {
    return when {
        isAtLeastU() -> {
            bindServiceInstance(
                caller,
                token,
                service,
                resolvedType,
                connection,
                flags.toLong(),
                instanceName,
                callingPackage,
                userId
            )
        }
        isAtLeastT() -> {
            bindServiceInstance(
                caller,
                token,
                service,
                resolvedType,
                connection,
                flags,
                instanceName,
                callingPackage,
                userId
            )
        }
        else -> {
            bindIsolatedService(
                caller,
                token,
                service,
                resolvedType,
                connection,
                flags,
                instanceName,
                callingPackage,
                userId
            )
        }
    }
}