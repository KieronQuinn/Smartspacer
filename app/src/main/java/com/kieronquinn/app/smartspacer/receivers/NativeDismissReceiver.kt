package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NativeDismissReceiver: BroadcastReceiver(), KoinComponent {

    private val settings by inject<SmartspacerSettingsRepository>()
    private val notifications by inject<NotificationRepository>()

    override fun onReceive(context: Context, intent: Intent) = runBlocking {
        settings.hasUsedNativeMode.set(false)
        notifications.cancelNotification(NotificationId.NATIVE_MODE)
    }

}