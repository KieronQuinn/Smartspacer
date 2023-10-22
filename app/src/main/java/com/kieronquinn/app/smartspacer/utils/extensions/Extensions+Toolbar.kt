package com.kieronquinn.app.smartspacer.utils.extensions

import android.view.View
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun Toolbar.onNavigationIconClicked() = callbackFlow<View> {
    setNavigationOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)