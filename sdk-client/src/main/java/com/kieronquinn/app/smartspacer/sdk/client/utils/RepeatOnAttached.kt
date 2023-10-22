package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun View.repeatOnAttached(block: suspend CoroutineScope.() -> Unit) {
    var launchedJob: Job? = null

    val mutex = Mutex()
    observeAttachedState { isAttached ->
        if (isAttached) {
            launchedJob = MainScope().launch(
                context = Dispatchers.Main.immediate,
                start = CoroutineStart.UNDISPATCHED
            ) {
                mutex.withLock {
                    coroutineScope {
                        block()
                    }
                }
            }
            return@observeAttachedState
        }
        launchedJob?.cancel()
        launchedJob = null
    }
}
