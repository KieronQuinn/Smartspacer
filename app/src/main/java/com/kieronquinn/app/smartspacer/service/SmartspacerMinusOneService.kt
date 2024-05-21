package com.kieronquinn.app.smartspacer.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.lifecycle.LifecycleService
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.ui.controllers.ConfigurationOverlayController
import com.kieronquinn.app.smartspacer.ui.screens.minusone.SmartspacerOverlay
import com.kieronquinn.app.smartspacer.utils.extensions.startForeground
import org.koin.android.ext.android.inject
import com.google.android.gsa.overlay.controllers.OverlayController as GsaOverlayController

class SmartspacerMinusOneService: LifecycleService() {

    private lateinit var overlaysController: OverlaysController
    private val notifications by inject<NotificationRepository>()

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationId.MINUS_ONE_SERVICE, createNotification())
        overlaysController = OverlayController()
    }

    override fun onDestroy() {
        overlaysController.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return overlaysController.onBind(intent, null)
    }

    override fun onUnbind(intent: Intent): Boolean {
        overlaysController.onUnbind(intent)
        return false
    }

    private inner class OverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(uid: Int, context: Context): GsaOverlayController {
            return SmartspacerOverlay(uid, context)
        }
    }

    private fun createNotification(): Notification {
        return notifications.showNotification(
            NotificationId.NATIVE_SERVICE,
            NotificationChannel.BACKGROUND_SERVICE
        ) {
            val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.BACKGROUND_SERVICE.id)
            }
            it.setContentTitle(getString(R.string.notification_title_background_service))
            it.setContentText(getString(R.string.notification_content_background_service))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.NATIVE_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_background_service))
        }
    }

}