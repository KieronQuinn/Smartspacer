package com.kieronquinn.app.smartspacer.ui.activities

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SafeModeReceiver
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.service.SmartspacerBackgroundService
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.workers.SmartspacerUpdateWorker
import com.kieronquinn.monetcompat.app.MonetCompatActivity
import org.koin.android.ext.android.inject

class MainActivity : MonetCompatActivity() {

    private val wiFiRepository by inject<WiFiRepository>()
    private val bluetoothRepository by inject<BluetoothRepository>()
    private val calendarRepository by inject<CalendarRepository>()

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

    override fun onResume() {
        super.onResume()
        //Check for changes to permission restricted repositories if the user has changed a perm
        wiFiRepository.refresh()
        bluetoothRepository.onPermissionChanged()
        //Alarm permission may have been granted
        calendarRepository.reloadEvents()
    }

}