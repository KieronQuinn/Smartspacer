package com.kieronquinn.app.smartspacer.utils.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

suspend fun <T> Flow<T?>.firstNotNull(): T {
    return first { it != null }!!
}

suspend inline fun <reified R> Flow<Any>.firstOfInstance(): R {
    return first { it is R } as R
}

fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = Observer<T> { value -> trySend(value) }
    observeForever(observer)
    awaitClose {
        removeObserver(observer)
    }
}.flowOn(Dispatchers.Main.immediate)

fun <T> Flow<T>.dropFirstUnless(shouldKeep: (T) -> Boolean): Flow<T> {
    var hasInitialised = false
    return dropWhile {
        if(shouldKeep(it)) return@dropWhile false
        !hasInitialised.also {
            hasInitialised = true
        }
    }
}

fun Flow<Boolean>.invertIf(shouldInvert: Boolean): Flow<Boolean> {
    return if(shouldInvert) {
        map { !it }
    }else this
}