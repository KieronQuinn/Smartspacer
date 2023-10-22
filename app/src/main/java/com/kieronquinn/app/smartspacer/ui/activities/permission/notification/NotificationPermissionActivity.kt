package com.kieronquinn.app.smartspacer.ui.activities.permission.notification

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationListenerService
import com.kieronquinn.app.smartspacer.utils.extensions.getNotificationListenerIntent
import com.kieronquinn.app.smartspacer.utils.extensions.wasInstalledWithSession
import org.koin.android.ext.android.inject

class NotificationPermissionActivity: AppCompatActivity() {

    private var hasCreated = false
    private val settings by inject<SmartspacerSettingsRepository>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(shouldShowUi()){
            setContentView(R.layout.activity_notification_permission)
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
            isNotificationPermissionGranted() -> {
                setResult(Activity.RESULT_OK)
                finish()
            }
            hasCreated && !shouldShowUi() -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            !hasCreated && !shouldShowUi() -> {
                startActivity(getNotificationListenerIntent(
                    SmartspacerNotificationListenerService::class.java
                ))
            }
        }
        hasCreated = true
    }

    //Intentionally duplicated from NotificationRepository to avoid spinning up the repo just for it
    private fun isNotificationPermissionGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(BuildConfig.APPLICATION_ID)
    }

}