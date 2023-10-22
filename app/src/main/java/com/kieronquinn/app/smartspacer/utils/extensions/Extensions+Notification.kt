package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.Notification
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

fun Notification.getContentTitle(): CharSequence? {
    return extras.getCharSequence(Notification.EXTRA_TITLE)
}

fun Notification.getContentText(): CharSequence? {
    return extras.getCharSequence(Notification.EXTRA_TEXT)
}

fun StatusBarNotification.isAppGroupCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        isAppGroup
    } else {
        notification.group != null && notification.sortKey != null
    }
}

fun NotificationCompat.Builder.getContentText(): CharSequence? {
    return this::class.java.getDeclaredField("mContentText").apply {
        isAccessible = true
    }.get(this) as CharSequence?
}