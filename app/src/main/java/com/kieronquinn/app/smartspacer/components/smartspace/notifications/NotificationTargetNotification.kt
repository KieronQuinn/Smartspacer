package com.kieronquinn.app.smartspacer.components.smartspace.notifications

import android.service.notification.StatusBarNotification
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.NotificationTarget.TargetData
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.getContentTitle
import org.koin.android.ext.android.inject

class NotificationTargetNotification: SmartspacerNotificationProvider() {

    private val notificationRepository by inject<NotificationRepository>()
    private val dataRepository by inject<DataRepository>()

    override fun onNotificationsChanged(
        smartspacerId: String,
        isListenerEnabled: Boolean,
        notifications: List<StatusBarNotification>
    ) {
        val settings = getSettings(smartspacerId) ?: return
        val newNotifications = notifications.groupBy {
            if(it.groupKey != null){
                "${it.packageName}:${it.groupKey}"
            }else{
                it.packageName
            }
        }.flatMap {
            if(it.key.contains(":")){
                listOf(it.value.lastOrNull())
            }else it.value
        }.filterNotNull().filterNot {
            it.notification?.getContentTitle() == null
        }.filter {
            !settings.hasChannels || settings.channels.contains(it.notification.channelId)
        }
        notificationRepository.setMirroredNotifications(smartspacerId, newNotifications)
        SmartspacerTargetProvider.notifyChange(
            provideContext(), NotificationTarget::class.java, smartspacerId
        )
    }

    override fun getConfig(smartspacerId: String): Config {
        val packages = getSettings(smartspacerId)?.packageName?.let { setOf(it) } ?: emptySet()
        return Config(packages)
    }

    private fun getSettings(smartspacerId: String): TargetData? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)
    }

}