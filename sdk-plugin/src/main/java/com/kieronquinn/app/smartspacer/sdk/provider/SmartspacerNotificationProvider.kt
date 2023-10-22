package com.kieronquinn.app.smartspacer.sdk.provider

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.callbacks.IResolveIntentCallback
import com.kieronquinn.app.smartspacer.sdk.utils.ParceledListSlice
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getProviderInfo

/**
 *  [SmartspacerNotificationProvider] is a provider that allows you to receive notifications from
 *  the system to your plugin, without needing to register and run your own Notification Listener
 *  Service.
 *
 *  You **must** specify a list of packages to receive notifications from, and the user must grant
 *  permission for your app to receive notifications when adding the Target or Complication this
 *  provider is attached to. If possible, try to keep the number of packages small, if you're
 *  looking to listen for a significant number of packages for notifications, consider using a
 *  regular Notification Listener Service for less overhead.
 *
 */
abstract class SmartspacerNotificationProvider: BaseProvider() {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_NOTIFICATIONS_CHANGED = "on_notifications_changed"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_CONFIG = "get_notification_config"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_IS_LISTENER_ENABLED = "is_listener_enabled"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_NOTIFICATIONS = "notifications"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SMARTSPACER_ID = "smartspace_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_DISMISS_NOTIFICATION = "dismiss_notification"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_RESOLVE_INTENT = "resolve_notification_intent"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_NOTIFICATION = "notification"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_CALLBACK = "callback"

        private const val AUTHORITY_SMARTSPACER_NOTIFICATION_PROVIDER =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.notificationprovider"

        /**
         *  Dismisses a given [StatusBarNotification]
         */
        fun dismissNotification(context: Context, notification: StatusBarNotification) {
            context.contentResolver.callRemote(
                METHOD_DISMISS_NOTIFICATION,
                bundleOf(EXTRA_NOTIFICATION to notification)
            )
        }

        /**
         *  Attempts to resolve a [StatusBarNotification]'s content intent, to an Intent. The
         *  [callback]'s [ResolveIntentCallback.onResult] method will be called when a result or
         *  `null` is available.
         *
         *  This requires enhanced mode to be enabled, and Shizuku to be running. If these
         *  requirements are not met, `null` will be returned, so do not rely solely on this
         *  call - use it only for extra information. You can use a PendingIntent for Target and
         *  Complication click events, no need to resolve the Intent.
         */
        fun resolveContentIntent(
            context: Context,
            notification: StatusBarNotification,
            callback: ResolveIntentCallback
        ) {
            context.contentResolver.callRemote(
                METHOD_RESOLVE_INTENT,
                bundleOf(
                    EXTRA_NOTIFICATION to notification,
                    EXTRA_CALLBACK to callback.asBinder()
                )
            )
        }

        /**
         *  Attempts to resolve a [StatusBarNotification]'s content intent, to an Intent. The lambda
         *  will be invoked when a result or `null` is available.
         *
         *  This requires enhanced mode to be enabled, and Shizuku to be running. If these
         *  requirements are not met, `null` will be returned, so do not rely solely on this
         *  call - use it only for extra information. You can use a PendingIntent for Target and
         *  Complication click events, no need to resolve the Intent.
         */
        fun resolveContentIntent(
            context: Context,
            notification: StatusBarNotification,
            callback: (Intent?) -> Any?
        ) {
            val callbackWrapper = object: ResolveIntentCallback() {
                override fun onResult(intent: Intent?) {
                    callback.invoke(intent)
                }
            }
            resolveContentIntent(context, notification, callbackWrapper)
        }

        private fun ContentResolver.callRemote(method: String, extras: Bundle? = null): Bundle? {
            val provider = acquireUnstableContentProviderClient(
                AUTHORITY_SMARTSPACER_NOTIFICATION_PROVIDER
            )
            return try {
                provider?.call(method, null, extras).also {
                    provider?.close()
                }
            }catch (e: Exception){
                //Provider has gone
                return null
            }catch (e: Error){
                //Provider has gone
                return null
            }
        }

        private fun findAuthority(
            context: Context,
            provider: Class<out SmartspacerNotificationProvider>
        ): String {
            return context.packageManager.getProviderInfo(ComponentName(context, provider))
                .authority
        }

        /**
         *  Notify Smartspacer of a change to a given [provider], and that notification config
         *  should be refreshed. Pass a [smartspacerId] to only refresh that provider, otherwise
         *  all providers of this type will be refreshed.
         */
        fun notifyChange(
            context: Context,
            provider: Class<out SmartspacerNotificationProvider>,
            smartspacerId: String?
        ) {
            val authority = findAuthority(context, provider)
            notifyChange(context, authority, smartspacerId)
        }

        /**
         *  Notify Smartspacer of a change to a given [authority], and that notification config
         *  should be refreshed,. Pass a [smartspacerId] to only refresh that provider, otherwise
         *  all providers of this type will be refreshed.
         */
        fun notifyChange(
            context: Context,
            authority: String,
            smartspacerId: String?
        ) {
            val uri = Uri.Builder().apply {
                scheme("content")
                authority(authority)
                if(smartspacerId != null) {
                    appendPath(smartspacerId)
                }
            }.build()
            context.contentResolver?.notifyChange(uri, null, 0)
        }
    }

    /**
     *  Called when a notification is posted, removed or changes, and if the listener is enabled or
     *  disabled.
     *
     *  [isListenerEnabled] indicates whether Smartspacer's Notification Listener Service is enabled
     *
     *  [notifications] contains a list of current [StatusBarNotification]s for your requested
     *  packages.
     *
     *  Note: If [isListenerEnabled] is false, you may wish to display a Target prompting the user
     *  to enable Smartspacer's Notification Listener Service, and link to the Settings page to
     *  do so on tap.
     */
    abstract fun onNotificationsChanged(
        smartspacerId: String,
        isListenerEnabled: Boolean,
        notifications: List<StatusBarNotification>
    )

    /**
     *  Return the [Config] for this provider, specifying the packages to listen for notifications
     *  from. [smartspacerId] represents the ID of the Target or Complication this Provider is
     *  attached to.
     */
    abstract fun getConfig(smartspacerId: String): Config

    final override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        verifySecurity()
        return when(method){
            METHOD_ON_NOTIFICATIONS_CHANGED -> {
                extras?.classLoader = ParceledListSlice::class.java.classLoader
                val isListenerEnabled = extras?.getBoolean(EXTRA_IS_LISTENER_ENABLED) ?: return null
                val smartspacerId = extras.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val notificationsSliced = extras.getParcelableCompat(
                    EXTRA_NOTIFICATIONS, ParceledListSlice::class.java
                ) as? ParceledListSlice<StatusBarNotification> ?: return null
                onNotificationsChanged(smartspacerId, isListenerEnabled, notificationsSliced.list)
                null
            }
            METHOD_GET_CONFIG -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                getConfig(smartspacerId).toBundle()
            }
            else -> null
        }
    }

    data class Config(
        /**
         *  The packages to listen for notifications from
         */
        val packages: Set<String>
    ) {

        companion object {
            private const val KEY_PACKAGES = "packages"
        }

        constructor(bundle: Bundle): this(
            bundle.getStringArrayList(KEY_PACKAGES)!!.toSet()
        )

        fun toBundle() = bundleOf(
            KEY_PACKAGES to ArrayList<String>(packages)
        )

    }

    abstract class ResolveIntentCallback: IResolveIntentCallback.Stub()

}