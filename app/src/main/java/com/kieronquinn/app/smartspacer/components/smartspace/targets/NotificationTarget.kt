package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.NotificationTargetNotification
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.TARGET_NOTIFICATION
import com.kieronquinn.app.smartspacer.utils.extensions.getContentText
import com.kieronquinn.app.smartspacer.utils.extensions.getContentTitle
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class NotificationTarget: SmartspacerTargetProvider() {

    companion object {
        private const val TARGET_ID_PREFIX = "notification_"
    }

    private val notificationRepository by inject<NotificationRepository>()
    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val settings = getSettings(smartspacerId) ?: return emptyList()
        return getNotifications(smartspacerId).also {
        }.map { it.toTarget() }.applyAppShortcuts(settings.packageName)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val settings = smartspacerId?.let { getSettings(it) }
        val appName = if(settings?.packageName != null){
            provideContext().packageManager.getPackageLabel(settings.packageName)
        }else null
        return Config(
            label = resources.getString(R.string.target_notification_label),
            description = getDescription(settings, appName),
            icon = AndroidIcon.createWithResource(
                provideContext(), R.drawable.ic_target_notifications
            ),
            notificationProvider = "${BuildConfig.APPLICATION_ID}.notifications.notification",
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), TARGET_NOTIFICATION
            ),
            allowAddingMoreThanOnce = true
        )
    }

    private fun getDescription(settings: TargetData?, appName: CharSequence?): String {
        return when {
            appName == null -> resources.getString(R.string.target_notification_description)
            settings?.channels?.isNotEmpty() == true -> {
                val channelCount = settings.channels.count()
                val plural = resources.getQuantityString(
                    R.plurals.target_notification_description_channels, channelCount
                )
                resources.getString(
                    R.string.target_notification_description_selected_with_channels,
                    appName,
                    channelCount,
                    plural
                )
            }
            else -> {
                resources.getString(R.string.target_notification_description_selected, appName)
            }
        }
    }

    private fun StatusBarNotification.toTarget(): SmartspaceTarget {
        val notification = notification
        return TargetTemplate.Basic(
            id = "$TARGET_ID_PREFIX$id",
            componentName = ComponentName(provideContext(), MainActivity::class.java),
            icon = Icon(notification.smallIcon),
            title = Text(notification.getContentTitle()?.toString()
                ?: notification.tickerText?.toString() ?: ""),
            subtitle = notification.getContentTextOrAppName(packageName)?.let { Text(it) },
            onClick = TapAction(pendingIntent = notification.contentIntent)
        ).create().apply {
            sourceNotificationKey = key
            isSensitive = notification.visibility == Notification.VISIBILITY_SECRET
            val actions = notification.actions?.filter {
                it.actionIntent != null
            }?.map {
                Shortcuts.Shortcut(
                    it.title,
                    null,
                    pendingIntent = it.actionIntent
                )
            }?.let {
                if(it.isNotEmpty()) Shortcuts(it) else null
            }
            expandedState = ExpandedState(
                shortcuts = actions
            )
        }
    }

    private fun Notification.getContentTextOrAppName(packageName: String): CharSequence? {
        return getContentText().takeIf { !it.isNullOrBlank() } ?:
            provideContext().packageManager.getPackageLabel(packageName)
    }

    /**
     *  Add App Shortcuts to the last Notification Target, since all the Notifications come from the
     *  same package, they're only required on the final target.
     */
    private fun List<SmartspaceTarget>.applyAppShortcuts(packageName: String?) = apply {
        if(packageName == null) return@apply
        lastOrNull()?.apply {
            val expandedState = expandedState ?: ExpandedState()
            this.expandedState = expandedState.copy(
                appShortcuts = ExpandedState.AppShortcuts(setOf(packageName))
            )
        }
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        //Get the raw notification ID used to construct this target
        val id = targetId.removePrefix(TARGET_ID_PREFIX).toIntOrNull() ?: return false
        //Find the associated notification for this ID, if it's still available
        val notification = getNotifications(smartspacerId).firstOrNull {
            it.id == id
        } ?: return false
        //Dismiss the notification via the provider
        SmartspacerNotificationProvider.dismissNotification(provideContext(), notification)
        return true
    }

    override fun onProviderRemoved(smartspacerId: String) {
        super.onProviderRemoved(smartspacerId)
        dataRepository.deleteTargetData(smartspacerId)
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = getSettings(smartspacerId) ?: return Backup()
        val appName = if(settings.packageName != null){
            provideContext().packageManager.getPackageLabel(settings.packageName)
        }else null
        return Backup(
            gson.toJson(settings),
            getDescription(settings, appName)
        )
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val settings = gson.fromJson(backup.data ?: return false, TargetData::class.java)
        dataRepository.updateTargetData(
            smartspacerId, TargetData::class.java, TargetDataType.NOTIFICATION, ::onBackupRestored
        ){
            settings
        }
        return true
    }

    private fun onBackupRestored(context: Context, smartspacerId: String) {
        SmartspacerNotificationProvider.notifyChange(
            context, NotificationTargetNotification::class.java, smartspacerId
        )
    }

    private fun getNotifications(smartspacerId: String): List<StatusBarNotification> {
        return (notificationRepository.mirroredNotifications[smartspacerId] ?: emptyList())
    }

    private fun getSettings(smartspacerId: String): TargetData? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)
    }

    data class TargetData(
        @SerializedName("package_name")
        val packageName: String? = null,
        @SerializedName("has_channels")
        val hasChannels: Boolean = false,
        @SerializedName("channels")
        val channels: Set<String> = emptySet()
    )

}