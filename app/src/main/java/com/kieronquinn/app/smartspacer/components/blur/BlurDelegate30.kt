package com.kieronquinn.app.smartspacer.components.blur

import android.app.ActivityManagerHidden
import android.content.Context
import android.os.Build
import android.os.SystemProperties
import android.view.SurfaceControl
import android.view.Window
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.utils.extensions.getViewRootImpl
import com.kieronquinn.app.smartspacer.utils.extensions.setBackgroundBlurRadius
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@RequiresApi(Build.VERSION_CODES.R)
class BlurDelegate30(
    private val mode: BlurMode,
    scope: CoroutineScope,
): BlurDelegate(mode, scope) {

    private val blurDisabledSysProp by lazy {
        SystemProperties.getBoolean("persist.sys.sf.disable_blurs", false)
    }

    private val supportsBackgroundBlur by lazy {
        SystemProperties.getBoolean("ro.surface_flinger.supports_background_blur", false)
    }

    private var blurEnabled = false

    override fun isModeSupported(mode: BlurMode): Boolean {
        return when (mode) {
            // Only window blurring is supported on 30, not blur behind
            is BlurMode.Window -> !mode.blurBehind
            else -> true
        }
    }

    override fun isBlurAvailable(context: Context): Flow<Boolean> {
        val result = when {
            !supportsBlursOnWindows() -> false
            mode is BlurMode.Window -> {
                // Test applying a blur to make sure it's working
                mode.window.applyBlur(lastRequestedBlur ?: 0f)
            }
            else -> true
        }
        blurEnabled = result
        return flowOf(result)
    }

    override fun applyBlur(ratio: Float) {
        if (!blurEnabled) {
            lastRequestedBlur = ratio
            return
        }
        when (mode) {
            is BlurMode.Window -> {
                mode.window.applyBlur(ratio)
                lastAppliedBlur = ratio
            }
            else -> super.applyBlur(ratio)
        }
    }

    private fun Window.applyBlur(ratio: Float): Boolean {
        val radius = blurRadiusOfRatio(ratio)
        runCatching {
            val viewRootImpl = decorView.getViewRootImpl() ?: return@runCatching
            val surfaceControl = viewRootImpl.surfaceControl ?: return@runCatching
            SurfaceControl.Transaction()
                .setBackgroundBlurRadius(surfaceControl, radius)
        }.onSuccess {
            return true
        }
        return false
    }

    private fun supportsBlursOnWindows(): Boolean {
        return supportsBackgroundBlur && !blurDisabledSysProp && ActivityManagerHidden.isHighEndGfx()
    }

}