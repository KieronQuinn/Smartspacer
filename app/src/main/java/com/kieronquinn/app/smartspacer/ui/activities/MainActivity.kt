package com.kieronquinn.app.smartspacer.ui.activities

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SafeModeReceiver
import com.kieronquinn.app.smartspacer.service.SmartspacerBackgroundService
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.workers.SmartspacerUpdateWorker
import com.kieronquinn.monetcompat.app.MonetCompatActivity

class MainActivity : MonetCompatActivity() {

    override val applyBackgroundColorToMenu = true

    companion object {
        const val EXTRA_SKIP_SPLASH = "SKIP_SPLASH"
    }

    override val applyBackgroundColorToWindow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        SmartspacerBackgroundService.startServiceIfNeeded(this)
        SmartspacerUpdateWorker.queueCheckWorker(this)
        DynamicColors.applyToActivityIfAvailable(this)
        SafeModeReceiver.dismissSafeModeNotificationIfShowing(this)
        whenCreated {
            monet.awaitMonetReady()
            setContentView(R.layout.activity_main)
        }
    }

}