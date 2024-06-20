package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import kotlinx.coroutines.flow.drop
import org.koin.android.ext.android.inject

class ExpandedActivity: MonetCompatActivity() {

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
        setupThemeForMinusOne()
        super.onCreate(savedInstanceState)
        setShowWhenLocked(!isMinusOne)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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