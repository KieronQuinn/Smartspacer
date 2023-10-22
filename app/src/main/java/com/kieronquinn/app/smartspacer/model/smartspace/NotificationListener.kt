package com.kieronquinn.app.smartspacer.model.smartspace

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.EXTRA_IS_LISTENER_ENABLED
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.EXTRA_NOTIFICATIONS
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.METHOD_GET_CONFIG
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.METHOD_ON_NOTIFICATIONS_CHANGED
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Config
import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.notificationServiceEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable

class NotificationListener(
    val context: Context,
    val id: String,
    val packageName: String,
    val authority: String
): KoinComponent, Closeable {

    private val scope = MainScope()

    private val packageRepository by inject<PackageRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val contentResolver = context.contentResolver
    private val serviceEnabled = context.notificationServiceEnabled()

    private val notifications = notificationRepository.activeNotifications

    private val idBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .appendPath(id)
        .build()

    private val authorityBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .build()

    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val appChange = packageRepository.onPackageChanged(scope, packageName)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteConfig = change.map {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val defaultConfig = Config(emptySet())

    private val requiredNotifications = combine(
        remoteConfig.filterNotNull(),
        notifications,
        serviceEnabled,
    ) { config, notifications, enabled ->
        val requiredApps = config.packages
        Pair(enabled, notifications.filter { requiredApps.contains(it.packageName) })
    }.distinctUntilChanged { old, new ->
        old.first == new.first && old.second.matches(new.second)
    }

    override fun close() {
        scope.cancel()
    }

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(METHOD_GET_CONFIG, bundleOf(EXTRA_SMARTSPACER_ID to id))
            ?: return@withContext defaultConfig
        if(config.isEmpty) return@withContext defaultConfig
        Config(config)
    }

    private suspend fun callRemote(
        method: String, extras: Bundle? = null
    ): Bundle? = withContext(Dispatchers.IO) {
        try {
            contentResolver?.callSafely(authority, method, null, extras)
        }catch (e: Throwable){
            //Provider has gone
            null
        }
    }

    private suspend fun sendNotifications(
        enabled: Boolean, notifications: List<StatusBarNotification>
    ) {
        val extras = bundleOf(
            EXTRA_IS_LISTENER_ENABLED to enabled,
            EXTRA_NOTIFICATIONS to ParceledListSlice(notifications),
            EXTRA_SMARTSPACER_ID to id
        )
        callRemote(METHOD_ON_NOTIFICATIONS_CHANGED, extras)
    }

    private fun setupNotifications() = scope.launch {
        requiredNotifications.debounce(500L).collect {
            sendNotifications(it.first, it.second)
        }
    }

    private fun List<StatusBarNotification>.matches(other: List<StatusBarNotification>): Boolean {
        if(size != other.size) return false
        //Unless two notifications somehow have identical times, we can simply check the post times.
        return map { it.postTime }.sorted() == other.map { it.postTime }.sorted()
    }

    init {
        setupNotifications()
    }

}