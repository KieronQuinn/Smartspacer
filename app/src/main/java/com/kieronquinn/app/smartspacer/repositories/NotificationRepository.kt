package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.RemoteException
import android.service.notification.StatusBarNotification
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.notifications.createNotification
import com.kieronquinn.app.smartspacer.model.smartspace.NotificationListener
import com.kieronquinn.app.smartspacer.receivers.StartShizukuReceiver
import com.kieronquinn.app.smartspacer.sdk.callbacks.IResolveIntentCallback
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationListenerService
import com.kieronquinn.app.smartspacer.service.SmartspacerNotificationWidgetService
import com.kieronquinn.app.smartspacer.utils.extensions.hasNotificationPermission
import com.kieronquinn.app.smartspacer.utils.extensions.isNotificationServiceEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.app.Notification as AndroidNotification
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel as NotificationsNotificationChannel
import com.kieronquinn.app.smartspacer.model.database.NotificationListener as DatabaseNotificationListener

interface NotificationRepository {

    /**
     *  All visible notifications, not filtered
     */
    val activeNotifications: StateFlow<List<StatusBarNotification>>

    /**
     *  Bus for notifications to be dismissed. This allows notification targets to dismiss the
     *  notification that has created the target, which will dismiss the target too.
     */
    val dismissNotificationBus: Flow<StatusBarNotification>

    /**
     *  All current Notification Listeners from plugins
     */
    val notificationListeners: StateFlow<List<DatabaseNotificationListener>>

    /**
     *  Mirrored notifications by Smartspacer Target ID
     */
    val mirroredNotifications: Map<String, List<StatusBarNotification>>

    /**
     *  Returns whether the app has notification permission (A13+, always true otherwise)
     */
    fun hasNotificationPermission(): Boolean

    /**
     *  Update the current list of active system notifications, called from the notification
     *  listener.
     */
    suspend fun updateNotifications(notifications: List<StatusBarNotification>)

    /**
     *  Dismiss a given notification
     */
    fun dismissNotification(notification: StatusBarNotification)

    /**
     *  Returns whether the notification listener service is enabled
     */
    fun isNotificationListenerEnabled(): Boolean

    /**
     *  Returns whether notification channels are available to be queried, ie. if the app has an
     *  association set
     */
    fun getNotificationChannelsAvailable(): Boolean

    /**
     *  Gets a list of notification channels for a given [packageName]
     */
    fun getNotificationChannelsForPackage(packageName: String): List<NotificationChannel>

    /**
     *  Gets a list of notification channel groups for a given [packageName]
     */
    fun getNotificationChannelGroupsForPackage(packageName: String): List<NotificationChannelGroup>

    /**
     *  Returns whether a given [packageName] has a notification listener registered
     */
    fun hasNotificationListener(packageName: String): Boolean

    /**
     *  Resolves a [StatusBarNotification]'s content intent, using the Shizuku service, if
     *  available.
     */
    fun resolveNotificationContentIntent(
        notification: StatusBarNotification, callback: IResolveIntentCallback
    )

    /**
     *  Sets the list of notifications in [mirroredNotifications] for a given [smartspacerId] to
     *  [notifications]
     */
    fun setMirroredNotifications(smartspacerId: String, notifications: List<StatusBarNotification>)

    /**
     *  Shows a notification built with [builder] immediately
     */
    fun showNotification(
        id: NotificationId,
        channel: NotificationsNotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): AndroidNotification

    /**
     *  Cancels a previously shown notification with a given [id]
     */
    fun cancelNotification(id: NotificationId)

    /**
     *  Shows a [NotificationId.SHIZUKU] notification with a given [content] string
     */
    fun showShizukuNotification(@StringRes content: Int)

}

class NotificationRepositoryImpl(
    private val context: Context,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    databaseRepository: DatabaseRepository,
    private val scope: CoroutineScope = MainScope()
): NotificationRepository {

    override val activeNotifications = MutableStateFlow(emptyList<StatusBarNotification>())
    override val dismissNotificationBus = MutableSharedFlow<StatusBarNotification>()
    override val mirroredNotifications = HashMap<String, List<StatusBarNotification>>()

    override val notificationListeners = databaseRepository.getNotificationListeners()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val companionManager =
        context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun hasNotificationPermission(): Boolean {
        return context.hasNotificationPermission()
    }

    override fun dismissNotification(notification: StatusBarNotification) {
        scope.launch {
            dismissNotificationBus.emit(notification)
        }
    }

    override fun isNotificationListenerEnabled(): Boolean {
        return context.isNotificationServiceEnabled()
    }

    override fun getNotificationChannelsAvailable(): Boolean {
        return companionManager.hasAssociatedDevices()
    }

    override fun getNotificationChannelsForPackage(packageName: String): List<NotificationChannel> {
        return SmartspacerNotificationListenerService.getAllNotificationChannels(packageName)
            ?.filterUncategorised() ?: emptyList()
    }

    override fun getNotificationChannelGroupsForPackage(packageName: String): List<NotificationChannelGroup> {
        return SmartspacerNotificationListenerService.getNotificationChannelGroups(packageName)
            ?: emptyList()
    }

    override fun hasNotificationListener(packageName: String): Boolean {
        return notificationListeners.value.any { it.packageName == packageName }
    }

    override fun resolveNotificationContentIntent(
        notification: StatusBarNotification,
        callback: IResolveIntentCallback
    ) {
        scope.launch {
            val intent = shizukuServiceRepository.runWithService {
                it.resolvePendingIntent(notification.notification.contentIntent)
            }.unwrap()
            try {
                callback.onResult(intent)
            }catch (e: RemoteException) {
                //Connection died
            }
        }
    }

    override fun setMirroredNotifications(
        smartspacerId: String,
        notifications: List<StatusBarNotification>
    ) {
        mirroredNotifications[smartspacerId] = notifications
    }

    override fun showNotification(
        id: NotificationId,
        channel: NotificationsNotificationChannel,
        builder: (NotificationCompat.Builder) -> Unit
    ): AndroidNotification {
        return context.createNotification(channel, builder).also {
            notificationManager.notify(id.ordinal, it)
        }
    }

    override suspend fun updateNotifications(notifications: List<StatusBarNotification>) {
        activeNotifications.emit(notifications)
    }

    override fun cancelNotification(id: NotificationId) {
        return notificationManager.cancel(id.ordinal)
    }

    @SuppressLint("LaunchActivityFromNotification")
    override fun showShizukuNotification(content: Int) {
        showNotification(
            NotificationId.SHIZUKU,
            NotificationsNotificationChannel.SHIZUKU
        ) {
            it.setContentTitle(context.getString(R.string.notification_shizuku_title))
            it.setContentText(context.getString(content))
            it.setSmallIcon(R.drawable.ic_warning)
            it.setOngoing(false)
            it.setAutoCancel(true)
            it.setContentIntent(
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.SHIZUKU.ordinal,
                    Intent(context, StartShizukuReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(context.getString(R.string.notification_shizuku_title))
        }
    }

    @Suppress("DEPRECATION")
    private fun CompanionDeviceManager.hasAssociatedDevices(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myAssociations
        } else {
            associations
        }.isNotEmpty()
    }

    private fun setupNotificationProviders() = scope.launch {
        var listeners = emptyList<NotificationListener>()
        //Keep an always up-to-date list of active listeners, destroying old ones as we go
        notificationListeners.collect {
            listeners.forEach { listener -> listener.close() }
            listeners = it.map { listener ->
                @Suppress("CloseNotificationListener")
                NotificationListener(context, listener.id, listener.packageName, listener.authority)
            }
        }
    }

    private fun setupNotificationWidgetService() = scope.launch {
        settingsRepository.notificationWidgetServiceEnabled.asFlow().collect { enabled ->
            if(enabled) {
                SmartspacerNotificationWidgetService.startServiceIfNeeded(context)
            }else{
                SmartspacerNotificationWidgetService.stopService(context)
            }
        }
    }

    /**
     *  Filter out the default "Uncategorized" channel, added when the app does not use channels,
     *  but only after the first notification has been shown. This is very confusing to users,
     *  it's much better to just have *no channels* visible, and fall back to showing all
     *  an app's notifications.
     */
    private fun List<NotificationChannel>.filterUncategorised(): List<NotificationChannel> {
        //Only applies to lists were there's exactly one channel
        if(size != 1) return this
        return filterNot { it.id == NotificationChannel.DEFAULT_CHANNEL_ID }
    }

    init {
        setupNotificationProviders()
        setupNotificationWidgetService()
    }

}