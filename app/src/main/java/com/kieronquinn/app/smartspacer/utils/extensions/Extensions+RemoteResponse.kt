package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse.RemoteResponse
import android.widget.RemoteViews.RemoteResponse as SystemRemoteResponse

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.getInteractionType(): Int {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return RemoteResponse.INTERACTION_TYPE_CLICK
    }
    return SystemRemoteResponse::class.java.getDeclaredField("mInteractionType").apply {
        isAccessible = true
    }.getInt(this)
}

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.getPendingIntent(): PendingIntent? {
    return SystemRemoteResponse::class.java.getDeclaredField("mPendingIntent").apply {
        isAccessible = true
    }.get(this) as? PendingIntent
}

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.getFillIntent(): Intent? {
    return SystemRemoteResponse::class.java.getDeclaredField("mFillIntent").apply {
        isAccessible = true
    }.get(this) as? Intent
}

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.getViewIds(): IntArray? {
    return SystemRemoteResponse::class.java.getDeclaredField("mViewIds").apply {
        isAccessible = true
    }.get(this) as? IntArray
}

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.getElementNames(): ArrayList<String>? {
    return SystemRemoteResponse::class.java.getDeclaredField("mElementNames").apply {
        isAccessible = true
    }.get(this) as? ArrayList<String>
}

fun SystemRemoteResponse.toRemoteResponse(): RemoteResponse {
    return RemoteResponse(
        getInteractionType(),
        getPendingIntent(),
        getFillIntent(),
        getViewIds()?.toList() ?: ArrayList(),
        getElementNames() ?: ArrayList()
    )
}

@SuppressLint("BlockedPrivateApi")
fun SystemRemoteResponse.setInteractionType(type: Int) = apply {
    SystemRemoteResponse::class.java.getDeclaredMethod("setInteractionType", Integer.TYPE)
        .apply { isAccessible = true }.invoke(this, type)
}
