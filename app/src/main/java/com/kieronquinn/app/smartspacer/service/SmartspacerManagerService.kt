package com.kieronquinn.app.smartspacer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.SmartspaceManager
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.utils.extensions.startForeground
import org.koin.android.ext.android.inject

class SmartspacerManagerService: Service() {

    private val smartspaceManager by inject<SmartspaceManager>()
    private val notifications by inject<NotificationRepository>()

    override fun onBind(intent: Intent): IBinder {
        return smartspaceManager
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationId.MANAGER_SERVICE, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        smartspaceManager.onServiceDestroy()
    }

    private fun createNotification(): Notification {
        return notifications.showNotification(
            NotificationId.MANAGER_SERVICE,
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
                    NotificationId.MANAGER_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_background_service))
        }
    }

}