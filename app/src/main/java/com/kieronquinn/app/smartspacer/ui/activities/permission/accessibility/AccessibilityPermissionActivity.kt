package com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.utils.extensions.getAccessibilityIntent
import com.kieronquinn.app.smartspacer.utils.extensions.wasInstalledWithSession
import org.koin.android.ext.android.inject

class AccessibilityPermissionActivity: AppCompatActivity() {

    private var hasCreated = false
    private val settings by inject<SmartspacerSettingsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(shouldShowUi()){
            setContentView(R.layout.activity_accessibility_permission)
        }else{
            setTheme(R.style.Theme_Smartspacer_Transparent)
        }
    }

    /**
     *  We only need to show the instructional UI on Tiramisu or above, when restricted settings are
     *  at play
     */
    private fun shouldShowUi(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !settings.isRestrictedModeDisabled.getSync()
                && !wasInstalledWithSession()
    }

    override fun onResume() {
        super.onResume()
        when {
            isServiceRunning() -> {
                setResult(Activity.RESULT_OK)
                finish()
            }
            hasCreated && !shouldShowUi() -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            !hasCreated && !shouldShowUi() -> {
                startActivity(getAccessibilityIntent(SmartspacerAccessibiltyService::class.java))
            }
        }
        hasCreated = true
    }

    private fun isServiceRunning(): Boolean {
        return SmartspacerAccessibiltyService.isRunning(this)
    }

}