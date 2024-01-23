package com.kieronquinn.app.smartspacer.components.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.utils.extensions.getContentText
import com.kieronquinn.app.smartspacer.utils.extensions.hasNotificationPermission
import android.app.NotificationChannel as AndroidNotificationChannel

fun Context.createNotification(
    channel: NotificationChannel,
    builder: (NotificationCompat.Builder) -> Unit
): Notification {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationChannel =
        AndroidNotificationChannel(
            channel.id,
            getString(channel.titleRes),
            channel.importance
        ).apply {
            description = getString(channel.descRes)
        }
    notificationManager.createNotificationChannel(notificationChannel)
    return NotificationCompat.Builder(this, channel.id).apply(builder).apply {
        val text = getContentText() ?: return@apply
        setStyle(NotificationCompat.BigTextStyle(this).bigText(text))
    }.build()
}

enum class NotificationChannel(
    val id: String,
    val importance: Int,
    val titleRes: Int,
    val descRes: Int
) {
    BACKGROUND_SERVICE(
        "background_service",
        NotificationManager.IMPORTANCE_DEFAULT,
        R.string.notification_channel_background_service_title,
        R.string.notification_channel_background_service_subtitle
    ),
    SAFE_MODE(
        "safe_mode",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_safe_mode_title,
        R.string.notification_channel_safe_mode_subtitle
    ),
    ERROR(
        "error",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_error_title,
        R.string.notification_channel_error_content
    ),
    ACCESSIBILITY(
        "accessibility",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_accessibility_title,
        R.string.notification_channel_accessibility_content
    ),
    NATIVE_MODE(
        "native_mode",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_native_title,
        R.string.notification_channel_native_content
    ),
    SHIZUKU(
        "shizuku",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_shizuku_reminder_title,
        R.string.notification_channel_shizuku_reminder_content
    ),
    OEM(
        "oem",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_oem_title,
        R.string.notification_channel_oem_content
    ),
    UPDATES(
        "updates",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_updates_title,
        R.string.notification_channel_updates_content
    ),
    PLUGIN_UPDATES(
        "updates",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_plugin_updates_title,
        R.string.notification_channel_plugin_updates_content
    ),
    WIDGET_NOTIFICATION(
        "widget_notification",
        NotificationManager.IMPORTANCE_MAX,
        R.string.notification_channel_widget_title,
        R.string.notification_channel_widget_content
    );

    fun isEnabled(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(!context.hasNotificationPermission()) return false
        if(!notificationManager.areNotificationsEnabled()) return false
        //If the channel hasn't been created yet, default to enabled
        val channel = notificationManager.getNotificationChannel(id) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}

enum class NotificationId {
    UNUSED,
    BACKGROUND_SERVICE,
    NATIVE_SERVICE,
    SAFE_MODE,
    BACKGROUND_LOCATION,
    ENABLE_ACCESSIBILITY,
    NATIVE_MODE,
    NATIVE_MODE_DISMISS,
    SHIZUKU,
    MANAGER_SERVICE,
    UPDATES,
    PLUGIN_UPDATES,
    MINUS_ONE_SERVICE,
    TIME_DATE_ALARM,
    CALENDAR_ALARM,
    DAILY_UPDATE_ALARM,
    BATTERY_OPTIMISATION,
    GREETING_ALARM,
    CALENDAR_TOTM_ALARM,
    ALARM_COMPLICATION_ALARM,
    WIDGET_DIRECTION,
    NATIVE_MODE_JUST_ENABLE,
    RECONNECT_PROMPT,
    RECONNECT_JUST_RECONNECT,
    NOTIFICATION_SERVICE,
    SMARTSPACER_WIDGET_NOTIFICATION,
    CLOCK_COMPLICATION,
    BLUETOOTH_REQUIRED,
    FLASHLIGHT
}