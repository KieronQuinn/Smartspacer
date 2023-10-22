package com.kieronquinn.app.smartspacer.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.notifications.createNotification
import com.kieronquinn.app.smartspacer.ui.activities.safemode.SafeModeActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity

class SafeModeReceiver: BroadcastReceiver() {

    companion object {
        const val KEY_CRASHED_PACKAGE = "crashed_package"

        fun dismissSafeModeNotificationIfShowing(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.cancel(NotificationId.SAFE_MODE.ordinal)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        val crashedPackage = intent.getStringExtra(KEY_CRASHED_PACKAGE) ?: return
        val crashedPackageName = context.packageManager.getPackageLabel(crashedPackage) ?: return
        context.createNotification(NotificationChannel.SAFE_MODE) {
            it.setSmallIcon(R.drawable.ic_notification_safe_mode)
            it.setContentTitle(context.getString(R.string.notification_safe_mode_title))
            it.setContentText(
                context.getString(R.string.notification_safe_mode_content, crashedPackageName)
            )
            it.setContentIntent(PendingIntent.getActivity(
                context,
                NotificationId.SAFE_MODE.ordinal,
                Intent(context, SafeModeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }.also {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.notify(NotificationId.SAFE_MODE.ordinal, it)
        }
    }

}