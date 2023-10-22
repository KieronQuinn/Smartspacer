package com.kieronquinn.app.smartspacer.utils.extensions

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation

suspend inline fun <T> suspendCoroutineWithTimeout(timeout: Long, crossinline block: (Continuation<T>) -> Unit ) : T? {
    var finalValue : T? = null
    withTimeoutOrNull(timeout) {
        finalValue = suspendCancellableCoroutine(block = block)
    }
    return finalValue
}

suspend inline fun <T> suspendCancellableCoroutineWithTimeout(timeout: Long, crossinline block: (CancellableContinuation<T>) -> Unit ) : T? {
    var finalValue : T? = null
    withTimeoutOrNull(timeout) {
        finalValue = suspendCancellableCoroutine(block = block)
    }
    return finalValue
}

fun <T> CoroutineScope.launch(lock: Mutex, block: suspend () -> T) {
    launch {
        lock.withLock {
            block()
        }
    }
}