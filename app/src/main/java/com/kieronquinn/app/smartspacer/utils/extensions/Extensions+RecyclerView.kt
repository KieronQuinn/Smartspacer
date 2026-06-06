package com.kieronquinn.app.smartspacer.utils.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

suspend fun RecyclerView.runAfterAnimationsFinished(block: () -> Unit) {
    if(isAnimating) {
        awaitPost()
        runAfterAnimationsFinished(block)
    }else{
        block()
    }
}

fun RecyclerView.firstVisibleItemPosition() = callbackFlow {
    val manager = layoutManager as LinearLayoutManager
    setOnScrollChangeListener { _, _, _, _, _ ->
        trySend(manager.findFirstCompletelyVisibleItemPosition())
    }
    trySend(manager.findFirstCompletelyVisibleItemPosition())
    awaitClose {
        setOnScrollChangeListener(null)
    }
}.distinctUntilChanged()