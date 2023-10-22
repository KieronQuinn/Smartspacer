package com.kieronquinn.app.smartspacer.providers

import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.callbacks.IResolveIntentCallback
import com.kieronquinn.app.smartspacer.sdk.provider.BaseProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.EXTRA_CALLBACK
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.EXTRA_NOTIFICATION
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.METHOD_DISMISS_NOTIFICATION
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerNotificationProvider.Companion.METHOD_RESOLVE_INTENT
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class SmartspacerNotificationProvider: BaseProvider() {

    private val notificationRepository by inject<NotificationRepository>()

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(method){
            METHOD_DISMISS_NOTIFICATION -> {
                val notification = extras?.getParcelableCompat(
                    EXTRA_NOTIFICATION, StatusBarNotification::class.java
                ) ?: return null
                if(!verifyCallingSecurity()) return null
                dismissNotification(notification)
                null
            }
            METHOD_RESOLVE_INTENT -> {
                val notification = extras?.getParcelableCompat(
                    EXTRA_NOTIFICATION, StatusBarNotification::class.java
                ) ?: return null
                val callback = extras.getBinder(EXTRA_CALLBACK)?.let {
                    IResolveIntentCallback.Stub.asInterface(it)
                } ?: return null
                if(!verifyCallingSecurity()) return null
                resolveNotificationIntent(notification, callback)
                null
            }
            else -> null
        }
    }

    private fun dismissNotification(notification: StatusBarNotification) {
        notificationRepository.dismissNotification(notification)
    }

    private fun resolveNotificationIntent(
        notification: StatusBarNotification,
        callback: IResolveIntentCallback
    ) {
        notificationRepository.resolveNotificationContentIntent(notification, callback)
    }

    /**
     *  Checks that the calling package has a registered notification listener.
     */
    private fun verifyCallingSecurity(): Boolean = runBlocking {
        val callingPackage = callingPackage ?: BuildConfig.APPLICATION_ID
        notificationRepository.hasNotificationListener(callingPackage)
    }

}