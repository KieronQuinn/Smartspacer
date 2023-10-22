package com.kieronquinn.app.smartspacer.sdksample.plugin.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider

/**
 *  Example of how to use [SmartspacerNotificationProvider]. This provider requests notifications
 *  from `com.matusmak.fakenotifications`, an app that can be used to test notifications:
 *
 *  [Link](https://play.google.com/store/apps/details?id=com.matusmak.fakenotifications)
 */
class ExampleNotificationProvider: SmartspacerNotificationProvider() {

    override fun onNotificationsChanged(
        smartspacerId: String,
        isListenerEnabled: Boolean,
        notifications: List<StatusBarNotification>
    ) {
        val notifText = notifications.joinToString(", ") {
            it.notification.getContentTitle() ?: ""
        }
        Log.d("ExampleNotif", "Listener $smartspacerId enabled: $isListenerEnabled, notifications: $notifText")
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(setOf("com.matusmak.fakenotifications"))
    }

    private fun Notification.getContentTitle(): CharSequence? {
        return extras.getCharSequence(Notification.EXTRA_TITLE)
    }

}