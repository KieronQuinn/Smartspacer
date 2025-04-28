package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
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
import com.kieronquinn.app.smartspacer.utils.extensions.isDarkMode
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
        return getNotifications(smartspacerId).mapNotNull { it.toTarget(settings) }
            .applyAppShortcuts(settings.packageName)
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

    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toTarget(data: TargetData): SmartspaceTarget? {
        val notification = notification
        return TargetTemplate.Basic(
            id = "$TARGET_ID_PREFIX$id",
            componentName = ComponentName(provideContext(), MainActivity::class.java),
            icon = Icon(notification.smallIcon),
            title = Text(notification.getContentTitle()?.toString()
                ?: notification.tickerText?.toString() ?: ""),
            subtitle = notification.getContentTextOrAppName(
                packageName,
                data.trimNewLines
            )?.let { Text(it) },
            onClick = TapAction(pendingIntent = notification.contentIntent)
        ).create().apply {
            val remoteViews = when(data.remoteViews) {
                TargetData.RemoteViews.NONE -> null
                TargetData.RemoteViews.SMALL -> {
                    notification.contentView
                }
                TargetData.RemoteViews.LARGE -> {
                    notification.bigContentView
                }
                TargetData.RemoteViews.HEADS_UP -> {
                    notification.headsUpContentView
                }
            }
            if(remoteViews != null) {
                val backgroundLayout = data.remoteViewsBackground.getLayout(
                    provideContext().isDarkMode
                )
                val backgroundRemoteViews = if(backgroundLayout != null) {
                    RemoteViews(
                        provideContext().packageName,
                        backgroundLayout
                    ).apply {
                        removeAllViews(R.id.remoteviews_background_wrapper)
                        addView(R.id.remoteviews_background_wrapper, remoteViews)
                    }
                }else remoteViews
                val paddedRemoteViews = RemoteViews(
                    provideContext().packageName,
                    data.remoteViewsPadding.layout
                ).apply {
                    removeAllViews(R.id.remoteviews_padding_wrapper)
                    addView(R.id.remoteviews_padding_wrapper, backgroundRemoteViews)
                    if(data.remoteViewsReplaceClick && notification.contentIntent != null) {
                        setOnClickPendingIntent(
                            R.id.remoteviews_padding_wrapper,
                            notification.contentIntent
                        )
                    }
                }
                this.remoteViews = paddedRemoteViews
            }else{
                this.remoteViews = null
            }
            sourceNotificationKey = key
            isSensitive = notification.visibility == Notification.VISIBILITY_SECRET
            val actions = notification.actions?.filter {
                it.isUsable()
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

    private fun Notification.Action.isUsable(): Boolean {
        if(actionIntent == null) return false
        if(!remoteInputs.isNullOrEmpty()) return false
        return dataOnlyRemoteInputs.isNullOrEmpty()
    }

    private fun Notification.getContentTextOrAppName(
        packageName: String,
        trimNewLines: Boolean
    ): CharSequence? {
        val text = getContentText().takeIf { !it.isNullOrBlank() } ?:
            provideContext().packageManager.getPackageLabel(packageName)
        return if(trimNewLines) {
            text?.toString()?.replace("\n", " ")
        }else text
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
        @SerializedName("trim_new_lines")
        val _trimNewLines: Boolean? = null,
        @SerializedName("channels")
        val channels: Set<String> = emptySet(),
        @SerializedName("remote_views")
        val _remoteViews: RemoteViews? = null,
        @SerializedName("remote_views_background")
        val _remoteViewsBackground: Background? = null,
        @SerializedName("remote_views_padding")
        val _remoteViewsPadding: Padding? = null,
        @SerializedName("remote_views_replace_click")
        val _remoteViewsReplaceClick: Boolean? = null
    ) {

        val trimNewLines: Boolean
            get() = _trimNewLines ?: false

        val remoteViews: RemoteViews
            get() = _remoteViews ?: RemoteViews.NONE

        val remoteViewsBackground: Background
            get() = _remoteViewsBackground ?: Background.SEMI

        val remoteViewsPadding: Padding
            get() = _remoteViewsPadding ?: Padding.MEDIUM

        val remoteViewsReplaceClick: Boolean
            get() = _remoteViewsReplaceClick ?: false

        enum class RemoteViews(@StringRes val label: Int) {
            NONE(R.string.target_notification_remoteviews_source_none),
            SMALL(R.string.target_notification_remoteviews_source_small),
            LARGE(R.string.target_notification_remoteviews_source_large),
            HEADS_UP(R.string.target_notification_remoteviews_source_heads_up),
        }

        enum class Background(
            @StringRes val label: Int,
            @LayoutRes val layoutLight: Int?,
            @LayoutRes val layoutDark: Int?
        ) {
            NONE(R.string.target_notification_remoteviews_background_none, null, null),
            SEMI(
                R.string.target_notification_remoteviews_background_semi,
                R.layout.remoteviews_wrapper_background_50_light,
                R.layout.remoteviews_wrapper_background_50_dark
            ),
            OPAQUE(
                R.string.target_notification_remoteviews_background_opaque,
                R.layout.remoteviews_wrapper_background_100_light,
                R.layout.remoteviews_wrapper_background_100_dark
            );

            fun getLayout(darkMode: Boolean): Int? {
                return if(darkMode) layoutDark else layoutLight
            }
        }

        enum class Padding(@StringRes val label: Int, @LayoutRes val layout: Int, val height: Int) {
            NONE(R.string.target_widget_padding_none, R.layout.remoteviews_wrapper_padding_none, 96),
            SMALL(R.string.target_widget_padding_small, R.layout.remoteviews_wrapper_padding_small, 92),
            MEDIUM(R.string.target_widget_padding_medium, R.layout.remoteviews_wrapper_padding_medium, 88),
            LARGE(R.string.target_widget_padding_large, R.layout.remoteviews_wrapper_padding_large, 80),
            DISABLED(R.string.target_widget_padding_disable, R.layout.remoteviews_wrapper_padding_disabled, 96),
        }

    }

}