package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.kieronquinn.app.smartspacer.utils.test.TestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion

fun ContentResolver.registerContentObserverSafely(
    uri: Uri,
    notifyForDescendants: Boolean,
    observer: ContentObserver
) {
    try{
        TestUtils.registerContentObserver?.invoke(uri, observer)
            ?: registerContentObserver(uri, notifyForDescendants, observer)
    }catch (e: Exception){
        //Does not exist
        e.printStackTrace()
    }
}

fun ContentResolver.unregisterContentObserverSafely(observer: ContentObserver) {
    try{
        TestUtils.unregisterContentObserver?.invoke(observer)
            ?: unregisterContentObserver(observer)
    }catch (e: Exception){
        //Does not exist
        e.printStackTrace()
    }
}

private var observerCount = 0

fun ContentResolver.observerAsFlow(uri: Uri) = callbackFlow {
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }
    trySend(Unit)
    registerContentObserverSafely(uri, true, observer)
    observerCount++
    awaitClose {
        observerCount--
        unregisterContentObserverSafely(observer)
    }
}.flowOn(Dispatchers.IO)

fun ContentResolver.queryAsFlow(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Flow<Cursor> {
    val client = acquireContentProviderClient(uri) ?: return flowOf()
    return observerAsFlow(uri).mapNotNull {
        try {
            client.query(uri, projection, selection, selectionArgs, sortOrder)
        }catch (e: DeadObjectException){
            null
        }catch (e: SecurityException){
            null
        }
    }.onCompletion {
        try {
            client.close()
        }catch (e: DeadObjectException){
            e.printStackTrace()
        }
    }
}

fun ContentResolver.unsafeQueryAsFlow(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Flow<Cursor> {
    return observerAsFlow(uri).mapNotNull {
        querySafely(uri, projection, selection, selectionArgs, sortOrder)
    }
}

fun ContentResolver.querySafely(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Cursor? {
    val client = acquireUnstableContentProviderClient(uri) ?: return null
    return try {
        client.query(uri, projection, selection, selectionArgs, sortOrder).also {
            client.close()
        }
    }catch (e: DeadObjectException){
        null
    }
}

fun ContentResolver.callSafely(
    authority: String,
    method: String,
    arg: String?,
    extras: Bundle?
): Bundle? {
    val client = acquireUnstableContentProviderClient(authority) ?: run {
        return null
    }
    return try {
        client.call(method, arg, extras).also {
            client.close()
        }
    }catch (e: DeadObjectException){
        null
    }
}

fun ContentResolver.callSafely(
    uri: Uri,
    method: String,
    arg: String?,
    extras: Bundle?
): Bundle? {
    val client = acquireUnstableContentProviderClient(uri) ?: return null
    return try {
        client.call(method, arg, extras).also {
            client.close()
        }
    }catch (e: DeadObjectException){
        null
    }
}