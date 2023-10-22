package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.service.notification.StatusBarNotification
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import com.kieronquinn.app.smartspacer.utils.extensions.createPackageContextOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.getContentTitle
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Base Now Playing Target: Reads notification from Now Playing, parses out the track and artist,
 *  displaying as title and subtitle. Retains icon and click intent from notification.
 */
abstract class NowPlayingTarget: SmartspacerTargetProvider() {

    protected val notifications by inject<NotificationRepository>()

    abstract val packageName: String
    abstract val targetPrefix: String

    open fun getExpandedState(expandedState: ExpandedState): ExpandedState = expandedState

    private val packageResources by lazy {
        provideContext().createPackageContextOrNull(packageName)?.resources
    }

    private val regex by lazy {
        val id = packageResources?.getIdentifier(
            "song_by_artist", "string", packageName
        ) ?: return@lazy null
        packageResources?.getString(id, "(.*)", "(.*)")?.toRegex() ?: return@lazy null
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return getWarningTarget() ?: getNotification(smartspacerId)?.let {
            createNotificationMirror(it)
        }?.let {
            listOf(it)
        } ?: emptyList()
    }

    private fun getNotification(smartspacerId: String): StatusBarNotification? {
        return notifications.mirroredNotifications[smartspacerId]?.firstOrNull()
    }

    private fun createNotificationMirror(original: StatusBarNotification): SmartspaceTarget? {
        val pattern = regex ?: return null
        val notification = original.notification
        val match = pattern.find(notification.getContentTitle() ?: "") ?: return null
        val track = match.groupValues[1]
        val artist = match.groupValues[2]
        return TargetTemplate.Basic(
            id = "${targetPrefix}_${original.id}",
            componentName = ComponentName(provideContext(), MainActivity::class.java),
            icon = Icon(notification.smallIcon, shouldTint = true),
            title = Text(track),
            subtitle = Text(artist),
            onClick = TapAction(pendingIntent = notification.contentIntent)
        ).create().apply {
            sourceNotificationKey = original.key
            isSensitive = notification.visibility == Notification.VISIBILITY_SECRET
            expandedState = getExpandedState(createExpandedState(notification))
        }
    }

    private fun createExpandedState(notification: Notification): ExpandedState {
        return ExpandedState(
            shortcuts = ExpandedState.Shortcuts(
                notification.actions.map {
                    Shortcut(
                        label = null,
                        icon = Icon(it.getIcon()),
                        pendingIntent = it.actionIntent
                    )
                }
            )
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        //Dismiss the notification that created this target, thus dismissing the target
        val notification = getNotification(smartspacerId) ?: return false
        SmartspacerNotificationProvider.dismissNotification(provideContext(), notification)
        return true
    }

    private fun getWarningTarget(): List<SmartspaceTarget>? {
        if(notifications.isNotificationListenerEnabled()) return null
        return listOf(
            TargetTemplate.Basic(
                id = "${targetPrefix}_warning",
                componentName = ComponentName(provideContext(), MainActivity::class.java),
                title = Text(provideContext().getString(R.string.target_notification_warning_title)),
                subtitle = Text(provideContext().getString(R.string.target_notification_warning_description)),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_now_playing)),
                onClick = TapAction(intent = Intent(provideContext(), NotificationPermissionActivity::class.java))
            ).create()
        )
    }

}