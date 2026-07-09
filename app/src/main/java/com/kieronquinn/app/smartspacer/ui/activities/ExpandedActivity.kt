package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.utils.extensions.DynamicMonet
import kotlinx.coroutines.flow.drop
import org.koin.android.ext.android.inject

class ExpandedActivity: AppCompatActivity() {

    companion object {
        private const val KEY_IS_OVERLAY = "is_overlay"
        private const val KEY_UID = "uid"
        private const val CLASS_MINUS_ONE =
            "com.kieronquinn.app.smartspacer.ui.activities.MinusOneExpandedActivity"

        fun createOverlayIntent(context: Context, uid: Int): Intent {
            return Intent(context, ExpandedActivity::class.java).apply {
                putExtra(KEY_IS_OVERLAY, true)
                putExtra(KEY_UID, uid)
            }
        }

        fun createExportedOverlayIntent(context: Context): Intent {
            return Intent().apply {
                component = ComponentName(
                    context.packageName,
                    "com.kieronquinn.app.smartspacer.ui.activities.ExportedExpandedActivity"
                )
            }
        }

        fun isOverlay(expandedActivity: ExpandedActivity): Boolean {
            return expandedActivity.intent.getBooleanExtra(KEY_IS_OVERLAY, false)
        }

        fun isMinusOne(expandedActivity: ExpandedActivity): Boolean {
            return expandedActivity.intent.component?.className == CLASS_MINUS_ONE
        }

        fun getUid(expandedActivity: ExpandedActivity): Int {
            return expandedActivity.intent.getIntExtra(KEY_UID, Process.myUid())
        }
    }

    private val monet = DynamicMonet.getInstance()
    private val settings by inject<SmartspacerSettingsRepository>()
    private val expandedRepository by inject<ExpandedRepository>()

    private val isMinusOne by lazy {
        intent.component?.className == CLASS_MINUS_ONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        setupThemeForExpanded()
        setupThemeForMinusOne()
        super.onCreate(savedInstanceState)
        setShowWhenLocked(!isMinusOne)
        // Edge-to-edge: let our layout extend behind both bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Force bars fully transparent — the theme sets the colours but Android 10+
        // re-applies a contrast scrim unless we explicitly disable it in code.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setupOverlayBackPress()
        whenCreated {
            if(launchMainIfRequired()) return@whenCreated
            monet.awaitMonetReady()
            if(isOverlay(this@ExpandedActivity)) {
                setContentView(R.layout.activity_expanded_overlay)
            }else{
                setContentView(R.layout.activity_expanded)
            }
        }
    }

    private fun setupOverlayBackPress() {
        if(!intent.getBooleanExtra(KEY_IS_OVERLAY, false)) return
        onBackPressedDispatcher.addCallback {
            notifyOverlayBackPress()
        }
    }

    /**
     * Only applies to the standalone (non-overlay, non-MinusOne) expanded activity.
     *
     * SOLID: SolidWindow overrides the Wallpaper base theme's transparent window background
     *        so the wallpaper never bleeds through before the fragment renders.
     * BLUR/SCRIM: BlurWindow disables translucency so setBackgroundBlurRadius() works.
     *
     * When launched as the Smart Launcher overlay (isOverlayEarly=true), both cases are
     * skipped — SmartspacerOverlay owns the background there.
     */
    private fun setupThemeForExpanded() {
        val isOverlayEarly = intent.getBooleanExtra(KEY_IS_OVERLAY, false)
        val isMinusOneEarly = intent.component?.className == CLASS_MINUS_ONE
        if (isOverlayEarly || isMinusOneEarly) return
        // SOLID: apply SolidWindow overlay — overrides the Wallpaper base theme's transparent
        //        window background and wallpaper flag with an opaque colorBackground window,
        //        so the wallpaper never shows through before the fragment renders.
        // BLUR/SCRIM: apply BlurWindow overlay — disables translucency so
        //        setBackgroundBlurRadius() works; keeps wallpaper visible via the base theme.
        when (settings.expandedBackground.getSync()) {
            ExpandedBackground.SOLID ->
                theme.applyStyle(R.style.ThemeOverlay_Smartspacer_SolidWindow, true)
            ExpandedBackground.BLUR, ExpandedBackground.SCRIM ->
                theme.applyStyle(R.style.ThemeOverlay_Smartspacer_BlurWindow, true)
        }
    }

    private fun setupThemeForMinusOne() {
        if(!isMinusOne) return
        settings.expandedBackground.getSync().setMinusOneTheme()
        whenCreated {
            //Minus one needs to restart on theme changes
            settings.expandedBackground.asFlow().drop(1).collect {
                finish()
            }
        }
    }

    private fun ExpandedBackground.setMinusOneTheme() {
        val newTheme = when(this) {
            ExpandedBackground.SOLID -> {
                R.style.Theme_Smartspacer_MinusOne
            }
            else -> {
                R.style.Theme_Smartspacer_Wallpaper
            }
        }
        setTheme(newTheme)
    }

    private fun notifyOverlayBackPress() = whenCreated {
        expandedRepository.onOverlayBackPressed()
    }

    private suspend fun launchMainIfRequired(): Boolean {
        if(!settings.hasSeenSetup.get()){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return true
        }
        return false
    }

}