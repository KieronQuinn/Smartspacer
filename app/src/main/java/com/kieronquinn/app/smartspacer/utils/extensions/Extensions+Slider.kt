package com.kieronquinn.app.smartspacer.utils.extensions

import com.google.android.material.slider.Slider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Slider.onChanged() = callbackFlow {
    val listener = Slider.OnChangeListener { _, value, fromUser ->
        if(fromUser){
            trySend(value)
        }
    }
    trySend(value)
    addOnChangeListener(listener)
    awaitClose {
        removeOnChangeListener(listener)
    }
}