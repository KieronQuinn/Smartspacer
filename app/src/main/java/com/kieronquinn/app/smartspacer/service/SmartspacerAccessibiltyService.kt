package com.kieronquinn.app.smartspacer.service

import android.app.NotificationManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.AccessibilityRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.utils.accessibility.LifecycleAccessibilityService
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import org.koin.android.ext.android.inject

class SmartspacerAccessibiltyService: LifecycleAccessibilityService() {

    companion object {
        fun isRunning(context: Context): Boolean {
            return SmartspacerBackgroundService.isUsingEnhancedModeAppListener ||
                    context.isServiceRunning(SmartspacerAccessibiltyService::class.java)
        }
    }

    private val accessibilityRepository by inject<AccessibilityRepository>()
    private val settings by inject<SmartspacerSettingsRepository>()

    override fun onCreate() {
        super.onCreate()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationId.ENABLE_ACCESSIBILITY.ordinal)
        whenCreated {
            settings.setRestrictedModeKnownDisabledIfNeeded()
            accessibilityRepository.setForegroundPackage("")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return
        if(event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if(!event.isFullScreen) return
        val packageName = event.packageName?.toString() ?: return
        if(packageName == "android") return
        whenCreated {
            accessibilityRepository.setForegroundPackage(packageName)
        }
    }

    override fun onInterrupt() {
        //No-op
    }

}