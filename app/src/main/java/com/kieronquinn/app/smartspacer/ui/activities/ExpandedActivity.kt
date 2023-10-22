package com.kieronquinn.app.smartspacer.ui.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import org.koin.android.ext.android.inject

class ExpandedActivity: MonetCompatActivity() {

    companion object {
        private const val KEY_IS_OVERLAY = "is_overlay"

        fun createOverlayIntent(context: Context): Intent {
            return Intent(context, ExpandedActivity::class.java).apply {
                putExtra(KEY_IS_OVERLAY, true)
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
    }

    private val settings by inject<SmartspacerSettingsRepository>()
    private val expandedRepository by inject<ExpandedRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
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