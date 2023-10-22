package com.kieronquinn.app.smartspacer.utils.extensions

import android.widget.CompoundButton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun CompoundButton.onChanged() = callbackFlow {
    val listener = CompoundButton.OnCheckedChangeListener { _, checked ->
        trySend(checked)
    }
    setOnCheckedChangeListener(listener)
    awaitClose {
        setOnCheckedChangeListener(null)
    }
}.debounce(TAP_DEBOUNCE)