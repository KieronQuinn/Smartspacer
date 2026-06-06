package com.kieronquinn.app.smartspacer.utils.extensions

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.S)
fun Window.crossBlurEnabled(scope: CoroutineScope) = callbackFlow {
    val listener = Consumer(::trySend)
    windowManager.addCrossWindowBlurEnabledListener(listener)
    awaitClose {
        windowManager.removeCrossWindowBlurEnabledListener(listener)
    }
}.stateIn(scope, SharingStarted.Eagerly, windowManager.isCrossWindowBlurEnabled)

fun Window.clearDimming() {
    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}

fun Window.addDimming() {
    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}