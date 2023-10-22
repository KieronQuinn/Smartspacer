package com.kieronquinn.app.smartspacer.components.smartspace.notifications

import android.service.notification.StatusBarNotification
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import org.koin.android.ext.android.inject

abstract class NowPlayingTargetNotification: SmartspacerNotificationProvider() {

    companion object {
        private const val NOW_PLAYING_CHANNEL_ID =
            "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    }

    abstract val packageName: String
    abstract val targetClass: Class<out SmartspacerTargetProvider>

    private val notificationRepository by inject<NotificationRepository>()

    override fun onNotificationsChanged(
        smartspacerId: String,
        isListenerEnabled: Boolean,
        notifications: List<StatusBarNotification>
    ) {
        val nowPlayingNotifications = notifications.filter {
            it.notification.channelId == NOW_PLAYING_CHANNEL_ID
        }
        notificationRepository.setMirroredNotifications(smartspacerId, nowPlayingNotifications)
        SmartspacerTargetProvider.notifyChange(provideContext(), targetClass, smartspacerId)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(setOf(packageName))
    }

}