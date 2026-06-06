package com.kieronquinn.app.smartspacer.components.blur

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

class BlurDelegateStub(
    mode: BlurMode,
    scope: CoroutineScope,
): BlurDelegate(mode, scope) {

    override fun isBlurAvailable(context: Context) = flowOf(false)
    override fun isModeSupported(mode: BlurMode) = false

    @SuppressLint("MissingSuperCall")
    override fun applyBlur(ratio: Float) {
        // No-op, unsupported
    }

}