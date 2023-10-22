package com.kieronquinn.app.smartspacer.sdk.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.utils.RemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getProviderInfo

/**
 *  [SmartspacerWidgetProvider] is a [ContentProvider] that handles widget events, for widgets
 *  hosted by Smartspacer. This allows plugins to translate regular app widgets into Smartspace
 *  targets or complications, without having to host the widget themselves.
 *
 *  The `smartspacerId` passed to this provider will match the ID of the target or complication it
 *  is attached to.
 *
 *  **Note**: These providers are not directly connected to your target or complication, and you
 *  must store the translated data within your plugin (either in-memory or persisted) and call
 *  the required notifyChange method for your main target or complication provider.
 */
abstract class SmartspacerWidgetProvider: BaseProvider() {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_WIDGET_CHANGED = "on_widget_changed"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_VIEW_DATA_CHANGED = "on_view_data_changed"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_ADAPTER_CONNECTED = "on_adapter_connected"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_INFO = "get_widget_info"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_CONFIG = "get_widget_config"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_REMOVED = "on_removed"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_RECONFIGURE_INTENT_SENDER = "get_reconfigure_intent_sender"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_APP_WIDGET_ID = "get_app_widget_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_CLICK_VIEW = "click_view"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_ADAPTER = "get_adapter"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_REMOTE_COLLECTION_ITEMS = "get_remote_collection_items"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_SIZED_REMOTE_VIEWS = "get_sized_remote_views"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_LAUNCH_FILL_IN_INTENT = "launch_fill_in_intent"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_REMOVE_ACTIONS = "remove_actions"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_INTENT_SENDER = "intent_sender"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_APP_WIDGET_ID = "app_widget_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_VIEW_IDENTIFIER = "view_identifier"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_VIEW_ID = "view_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SIZE_F = "size_f"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SMARTSPACER_ID = "smartspace_id"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_REMOTE_VIEWS = "remote_views"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_REMOTE_ADAPTER = "remote_adapter"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_APP_WIDGET_PROVIDER_INFO = "app_widget_provider_info"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_INTENT = "intent"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_PENDING_INTENT = "pending_intent"

        private const val AUTHORITY_SMARTSPACER_WIDGET_PROVIDER =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.appwidgetprovider"

        /**
         *  Gets an [IntentSender] for a widget bound to this [smartspacerId]. This requires Android
         *  12, same as the system feature backing it.
         *
         *  If there is no widget bound to [smartspacerId] or the ID does not belong to your
         *  calling application, this will return `null`
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun getReconfigureIntentSender(context: Context, smartspacerId: String): IntentSender? {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            return context.contentResolver.callRemote(
                METHOD_GET_RECONFIGURE_INTENT_SENDER,
                bundleOf(EXTRA_SMARTSPACER_ID to smartspacerId)
            )?.getParcelableCompat(EXTRA_INTENT_SENDER, IntentSender::class.java)
        }

        /**
         *  Gets the app widget ID for a given [smartspacerId]. There are limited uses for this,
         *  since Smartspacer handles widget binding and loading for you, but you may need it to
         *  broadcast actions directly to the widget's app, if it allows it.
         *
         *  If there is no widget bound to [smartspacerId] or the ID does not belong to your
         *  calling application, this will return `null`
         */
        fun getAppWidgetId(context: Context, smartspacerId: String): Int? {
            return context.contentResolver.callRemote(
                METHOD_GET_APP_WIDGET_ID,
                bundleOf(EXTRA_SMARTSPACER_ID to smartspacerId)
            )?.getInt(EXTRA_APP_WIDGET_ID)
        }

        /**
         *  Clicks a view with the given [identifier] for the widget bound to [smartspacerId]. This
         *  allows you to invoke widget PendingIntents without reloading the whole view.
         *
         *  [identifier] should be in the format `$PACKAGE_NAME:id/$VIEW_ID`, where `VIEW_ID` is a
         *  string such as "content". See [View.findViewByIdentifier] for more info.
         *
         *  If there is no widget bound to [smartspacerId] or the ID does not belong to your
         *  calling application, this will do nothing.
         */
        fun clickView(context: Context, smartspacerId: String, identifier: String) {
            context.contentResolver.callRemote(
                METHOD_CLICK_VIEW,
                bundleOf(
                    EXTRA_SMARTSPACER_ID to smartspacerId,
                    EXTRA_VIEW_IDENTIFIER to identifier
                )
            )
        }

        /**
         *  Clicks a view with the given raw [viewId] for the widget bound to [smartspacerId]. This
         *  allows you to invoke widget PendingIntents without reloading the whole view.
         *
         *  If the [viewId] is not static, it's recommended you persist the last ID from loading
         *  the widget, to pass to this method. If it is static, consider using the equivalent
         *  method with an identifier instead.
         *
         *  If there is no widget bound to [smartspacerId] or the ID does not belong to your
         *  calling application, this will do nothing.
         */
        fun clickView(context: Context, smartspacerId: String, viewId: Int) {
            context.contentResolver.callRemote(
                METHOD_CLICK_VIEW,
                bundleOf(
                    EXTRA_SMARTSPACER_ID to smartspacerId,
                    EXTRA_VIEW_ID to viewId
                )
            )
        }

        /**
         *  Launches a [PendingIntent] with a [Intent] fill in intent, using Smartspacer. This
         *  allows you to "click" list items, once you have retrieved both the AdapterView's
         *  [pendingIntent], and the item's [fillInIntent]
         */
        fun launchFillInIntent(
            context: Context,
            smartspacerId: String,
            pendingIntent: PendingIntent,
            fillInIntent: Intent
        ) {
            context.contentResolver.callRemote(
                METHOD_LAUNCH_FILL_IN_INTENT,
                bundleOf(
                    EXTRA_SMARTSPACER_ID to smartspacerId,
                    EXTRA_PENDING_INTENT to pendingIntent,
                    EXTRA_INTENT to fillInIntent
                )
            )
        }

        private fun ContentResolver.callRemote(method: String, extras: Bundle? = null): Bundle? {
            val provider = acquireUnstableContentProviderClient(
                AUTHORITY_SMARTSPACER_WIDGET_PROVIDER
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
            provider: Class<out SmartspacerWidgetProvider>
        ): String {
            return context.packageManager.getProviderInfo(ComponentName(context, provider))
                .authority
        }

        /**
         *  Notify Smartspacer of a change to a given [provider], and that widget config should be
         *  refreshed, and an update requested on the widget. Pass a [smartspacerId] to only refresh
         *  that provider, otherwise all providers of this type will be refreshed.
         */
        fun notifyChange(
            context: Context,
            provider: Class<out SmartspacerWidgetProvider>,
            smartspacerId: String?
        ) {
            val authority = findAuthority(context, provider)
            notifyChange(context, authority, smartspacerId)
        }

        /**
         *  Notify Smartspacer of a change to a given [authority], and that widget config should be
         *  refreshed, and an update requested on the widget. Pass a [smartspacerId] to only refresh
         *  that provider, otherwise all providers of this type will be refreshed.
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
     *  Called when the widget changes. [remoteViews] contains the [RemoteViews] usually rendered
     *  into a widget, use [RemoteViews.apply] to load the raw Views to extract and transform data
     *  from them. [smartspacerId] is the ID of the target or complication this provider is
     *  attached to.
     */
    abstract fun onWidgetChanged(
        smartspacerId: String,
        remoteViews: RemoteViews?
    )

    /**
     *  Called when [AppWidgetManager.notifyAppWidgetViewDataChanged] is called for the widget.
     *  The [viewIdentifier] is the identifier of the view whose adapter has triggered this change,
     *  if available, or alternatively the [viewId] is the raw view ID, if available.
     */
    open fun onViewDataChanged(smartspacerId: String, viewIdentifier: String?, viewId: Int?) {
        //No-op by default
    }

    /**
     *  Return the [Config] for this provider, for a given [smartspacerId], which is the ID of the
     *  target of complication this provider is associated with.
     */
    abstract fun getConfig(smartspacerId: String): Config

    /**
     *  The [AppWidgetProviderInfo] of the App Widget this provider should handle, as it is
     *  defined in the manifest of the app providing the widget. Smartspacer will automatically
     *  bind the widget, calling the configure activity for the widget if required - you do not
     *  need to handle app widget IDs. The [smartspacerId] passed here is the ID of the associated
     *  target or complication for this provider.
     *
     *  This will only be called once during setup, and is committed to Smartspacer's database.
     *
     *  If you are looking to make a target or complication that allows users to dynamically select
     *  a widget, users must delete and add a new target/complication to change widget info.
     *
     *  For devices running Android 12 or above, you can call [getReconfigureIntentSender] to
     *  retrieve the [IntentSender] to re-launch the configure activity for widgets that allow
     *  this.
     */
    abstract fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo?

    /**
     *  Called when this provider has been removed, either by deletion of the attached
     *  target/complication, or if the user has canceled adding the target/complication during setup
     */
    open fun onProviderRemoved(smartspacerId: String) {
        //No-op by default
    }

    /**
     *  Called after [getAdapter] is called and the remote adapter is connected. From this call,
     *  you have about 10 seconds to process view data before the adapter is disconnected
     *  automatically. You must call [getAdapter] again if you wish to load more or newer data.
     */
    open fun onAdapterConnected(smartspacerId: String, adapter: RemoteAdapter) {
        //No-op by default
    }

    /**
     *  On Android 12+, RemoteViews can contain layouts for multiple sizes, but Android does
     *  not expose this data (it's behind @hide). To prevent plugins from having to call hidden
     *  methods, calling this will invoke the method for you and return the [RemoteViews] it
     *  returned.
     *
     *  On Android < 12, this will simply return the [RemoteViews] passed in [remoteViews].
     */
    protected fun getSizedRemoteView(remoteViews: RemoteViews, sizeF: SizeF): RemoteViews? {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return remoteViews
        return provideContext().contentResolver.callRemote(
            METHOD_GET_SIZED_REMOTE_VIEWS,
            bundleOf(EXTRA_REMOTE_VIEWS to remoteViews.copy(), EXTRA_SIZE_F to sizeF)
        )?.getParcelableCompat(EXTRA_REMOTE_VIEWS, RemoteViews::class.java)
    }

    /**
     *  Removes all actions which would be applied to a given View with [identifier] for this
     *  [RemoteViews], returning the modified [RemoteViews] without the actions. This can be used
     *  to remove actions which slow down View loading, or simply do not work (eg. they cause
     *  crashes)
     */
    protected fun removeActionsForView(remoteViews: RemoteViews, identifier: String): RemoteViews? {
        return provideContext().contentResolver.callRemote(
            METHOD_REMOVE_ACTIONS,
            bundleOf(EXTRA_REMOTE_VIEWS to remoteViews, EXTRA_VIEW_IDENTIFIER to identifier)
        )?.getParcelableCompat(EXTRA_REMOTE_VIEWS, RemoteViews::class.java)
    }

    /**
     *  Removes all actions which would be applied to a given View with raw [id] for this
     *  [RemoteViews], returning the modified [RemoteViews] without the actions. This can be used
     *  to remove actions which slow down View loading, or simply do not work (eg. they cause
     *  crashes)
     */
    protected fun removeActionsForView(remoteViews: RemoteViews, id: Int): RemoteViews? {
        return provideContext().contentResolver.callRemote(
            METHOD_REMOVE_ACTIONS,
            bundleOf(EXTRA_REMOTE_VIEWS to remoteViews, EXTRA_VIEW_ID to id)
        )?.getParcelableCompat(EXTRA_REMOTE_VIEWS, RemoteViews::class.java)
    }

    final override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        verifySecurity()
        return when(method){
            METHOD_ON_WIDGET_CHANGED -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val remoteViews = extras.getParcelableCompat(
                    EXTRA_REMOTE_VIEWS, RemoteViews::class.java
                )
                onWidgetChanged(smartspacerId, remoteViews)
                null
            }
            METHOD_ON_VIEW_DATA_CHANGED -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val viewIdentifier = extras.getString(EXTRA_VIEW_IDENTIFIER)
                val viewId = extras.getInt(EXTRA_VIEW_ID, -1).takeIf { it > 0 }
                if(viewIdentifier == null && viewId == null) return null
                onViewDataChanged(smartspacerId, viewIdentifier, viewId)
                null
            }
            METHOD_ON_ADAPTER_CONNECTED -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val adapter = RemoteAdapter(
                    extras.getBundle(EXTRA_REMOTE_ADAPTER) ?: return null
                )
                onAdapterConnected(smartspacerId, adapter)
                null
            }
            METHOD_GET_CONFIG -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                getConfig(smartspacerId).toBundle()
            }
            METHOD_GET_INFO -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                bundleOf(
                    EXTRA_APP_WIDGET_PROVIDER_INFO to getAppWidgetProviderInfo(smartspacerId)
                )
            }
            METHOD_ON_REMOVED -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                onProviderRemoved(smartspacerId)
                null
            }
            else -> null
        }
    }

    protected fun RemoteViews.load(): View? {
        return try {
            apply(provideContext(), null)
        }catch (e: Exception) {
            Log.d(this::class.java.simpleName, "Error loading RemoteViews", e)
            null
        }
    }

    /**
     *  Gets the adapter attached to a view of the widget with a given [viewIdentifier], in the
     *  format `$PACKAGE_NAME:id/$VIEW_ID`, where `VIEW_ID` is a string such as "list".
     *
     *  If you do not have a view ID, for example if this widget is using Jetpack Glance, use the
     *  equivalent method which takes an integer ID instead, providing the ID retrieved from a
     *  ViewStructure.
     *
     *  When the items are loaded, [onAdapterConnected] will be called with the [smartspacerId] and
     *  the adapter, containing [viewIdentifier].
     */
    protected fun getAdapter(smartspacerId: String, viewIdentifier: String) {
        provideContext().contentResolver.callRemote(
            METHOD_GET_ADAPTER,
            bundleOf(
                EXTRA_SMARTSPACER_ID to smartspacerId,
                EXTRA_VIEW_IDENTIFIER to viewIdentifier
            )
        )
    }

    /**
     *  Gets the adapter attached to a view of the widget with a given [viewId], which must be a
     *  raw ID **from the widget app** (not your plugin). Use the ViewStructure API to retrieve
     *  these IDs if the widget does not have static IDs (for example if it is using Jetpack Glance).
     *
     *  If you do have static IDs, use the equivalent method and pass an identifier instead.
     *
     *  When the items are loaded, [onAdapterConnected] will be called with the [smartspacerId] and
     *  the adapter, containing [viewId].
     */
    protected fun getAdapter(smartspacerId: String, viewId: Int) {
        provideContext().contentResolver.callRemote(
            METHOD_GET_ADAPTER,
            bundleOf(
                EXTRA_SMARTSPACER_ID to smartspacerId,
                EXTRA_VIEW_ID to viewId
            )
        )
    }

    /**
     *  Gets the `RemoteCollectionItems` attached to a view of the widget with a given
     *  [viewIdentifier], in the format `$PACKAGE_NAME:id/$VIEW_ID`, where `VIEW_ID` is a string
     *  such as "list". The collection items are treated like a regular adapter, for compatibility.
     *
     *  If you do not have a view ID, for example if this widget is using Jetpack Glance, use the
     *  equivalent method which takes an integer ID instead, providing the ID retrieved from a
     *  ViewStructure.
     *
     *  When the items are loaded, [onAdapterConnected] will be called with the [smartspacerId] and
     *  the adapter, containing [viewIdentifier].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    protected fun getRemoteCollectionItems(
        smartspacerId: String,
        viewIdentifier: String
    ) {
        provideContext().contentResolver.callRemote(
            METHOD_GET_REMOTE_COLLECTION_ITEMS,
            bundleOf(
                EXTRA_SMARTSPACER_ID to smartspacerId,
                EXTRA_VIEW_IDENTIFIER to viewIdentifier
            )
        )
    }

    /**
     *  Gets the `RemoteCollectionItems` attached to a view of the widget with a given
     *  [viewId], which must be a raw ID **from the widget app** (not your plugin). Use the
     *  ViewStructure API to retrieve these IDs if the widget does not have static IDs (for example
     *  if it is using Jetpack Glance).
     *
     *  If you do have static IDs, use the equivalent method and pass an identifier instead.
     *
     *  When the items are loaded, [onAdapterConnected] will be called with the [smartspacerId] and
     *  the adapter, containing [viewId].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    protected fun getRemoteCollectionItems(
        smartspacerId: String,
        viewId: Int
    ) {
        provideContext().contentResolver.callRemote(
            METHOD_GET_REMOTE_COLLECTION_ITEMS,
            bundleOf(
                EXTRA_SMARTSPACER_ID to smartspacerId,
                EXTRA_VIEW_ID to viewId
            )
        )
    }

    data class Config(
        /**
         *  The [width] of the widget sent to the provider, used in sending the correct
         *  [RemoteViews] in response. Defaults to the device's display portrait width when not set.
         */
        val width: Int? = null,
        /**
         *  The [height] of the widget sent to the provider, used in sending the correct
         *  [RemoteViews] in response. Defaults to a quarter of the device's display portrait height
         *  when not set.
         */
        val height: Int? = null
    ) {

        companion object {
            private const val KEY_WIDTH = "width"
            private const val KEY_HEIGHT = "height"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        constructor(bundle: Bundle): this(
            bundle.getInt(KEY_WIDTH),
            bundle.getInt(KEY_HEIGHT)
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toBundle(): Bundle {
            return bundleOf(
                KEY_WIDTH to width,
                KEY_HEIGHT to height
            )
        }

    }

}