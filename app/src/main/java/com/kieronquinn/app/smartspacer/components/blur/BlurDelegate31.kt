package com.kieronquinn.app.smartspacer.components.blur

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.kieronquinn.app.smartspacer.utils.extensions.crossBlurEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@RequiresApi(Build.VERSION_CODES.S)
class BlurDelegate31(
    private val mode: BlurMode,
    private val scope: CoroutineScope,
): BlurDelegate(mode, scope) {

    companion object {
        //Hidden in system - WHY?!?
        private const val SETTING_DISABLE_WINDOW_BLURS = "disable_window_blurs"
    }

    private val crossBlurEnabled = mode.window.crossBlurEnabled(scope)

    override fun isBlurAvailable(context: Context): Flow<Boolean> {
        return combine(
            crossBlurEnabled,
            context.disableWindowBlurs()
        ) { crossBlurEnabled, blursDisabled ->
            crossBlurEnabled && !blursDisabled
        }
    }

    override fun isModeSupported(mode: BlurMode): Boolean {
        return true // All modes supported
    }

    override fun applyBlur(ratio: Float) {
        if (!crossBlurEnabled.value) {
            lastRequestedBlur = ratio
            return
        }
        when (mode) {
            is BlurMode.Window -> {
                val radius = blurRadiusOfRatio(ratio)
                if (mode.blurBehind) {
                    mode.window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    mode.window.attributes.blurBehindRadius = radius
                    // Re-apply attributes
                    mode.window.attributes = mode.window.attributes
                } else {
                    mode.window.setBackgroundBlurRadius(radius)
                }
                lastAppliedBlur = ratio
            }
            is BlurMode.Background -> {
                val radius = blurRadiusOfRatio(ratio)
                mode.setBlurRadius(radius)
                lastAppliedBlur = ratio
            }
            else -> super.applyBlur(ratio)
        }
    }

    private fun Context.disableWindowBlurs(): Flow<Boolean> {
        val get = {
            Settings.Global.getInt(contentResolver, SETTING_DISABLE_WINDOW_BLURS) == 1
        }
        return try {
            contentResolver.observerAsFlow(
                Settings.Global.getUriFor(SETTING_DISABLE_WINDOW_BLURS)
            ).map {
                get()
            }.stateIn(scope, SharingStarted.Eagerly, get())
        }catch (e: Settings.SettingNotFoundException) {
            flowOf(false)
        }
    }

}