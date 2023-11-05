/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.widget

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfoHidden
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.IntentSender
import android.os.*
import com.android.internal.appwidget.IAppWidgetHost
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.resetAppWidget
import com.kieronquinn.app.smartspacer.utils.extensions.setInteractionHandler
import com.kieronquinn.app.smartspacer.utils.extensions.viewDataChanged
import dev.rikka.tools.refine.Refine
import java.lang.ref.WeakReference
import java.util.UUID

/**
 *  AppWidgetHost which supports multiple of the same app widget ID, shared across multiple views.
 *
 *  Due to not exposing the methods required, for older Android versions we have to copy and modify
 *  the entire class. However, due to the newer OS supporting a simpler modification, this should
 *  be future-proofed.
 *
 *  Converted into Kotlin for ease of editing.
 */
open class SmartspacerAppWidgetHostCompat @JvmOverloads constructor(
    context: Context,
    private val mHostId: Int,
    looper: Looper = context.mainLooper
) {
    private val mDisplayMetrics = context.resources.displayMetrics
    private val mContextOpPackageName = context.opPackageName
    private val mHandler = UpdateHandler(looper)
    private val mCallbacks = Callbacks(mHandler)
    private val mViews = HashMap<ListenerKey, AppWidgetHostView>()

    internal class Callbacks(handler: Handler) : IAppWidgetHost.Stub() {

        private val mWeakHandler = WeakReference(handler)

        override fun updateAppWidget(appWidgetId: Int, views: RemoteViews?) {
            var localViews = views
            if (isLocalBinder && views != null) {
                localViews = RemoteViews(views)
            }
            val handler = mWeakHandler.get() ?: return
            val msg = handler.obtainMessage(HANDLE_UPDATE, appWidgetId, 0, localViews)
            msg.sendToTarget()
        }

        override fun providerChanged(appWidgetId: Int, info: AppWidgetProviderInfo?) {
            var localInfo = info
            if (isLocalBinder && info != null) {
                localInfo = info.clone()
            }
            val handler = mWeakHandler.get() ?: return
            val msg = handler.obtainMessage(
                HANDLE_PROVIDER_CHANGED, appWidgetId, 0, localInfo
            )
            msg.sendToTarget()
        }

        override fun appWidgetRemoved(appWidgetId: Int) {
            val handler = mWeakHandler.get() ?: return
            handler.obtainMessage(HANDLE_APP_WIDGET_REMOVED, appWidgetId, 0).sendToTarget()
        }

        override fun providersChanged() {
            val handler = mWeakHandler.get() ?: return
            handler.obtainMessage(HANDLE_PROVIDERS_CHANGED).sendToTarget()
        }

        override fun viewDataChanged(appWidgetId: Int, viewId: Int) {
            val handler = mWeakHandler.get() ?: return
            val msg = handler.obtainMessage(
                HANDLE_VIEW_DATA_CHANGED, appWidgetId, viewId
            )
            msg.sendToTarget()
        }

        companion object {
            private val isLocalBinder: Boolean
                get() = Process.myPid() == getCallingPid()
        }
    }

    internal inner class UpdateHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HANDLE_UPDATE -> {
                    updateAppWidgetView(msg.arg1, msg.obj as? RemoteViews)
                }
                HANDLE_APP_WIDGET_REMOVED -> {
                    dispatchOnAppWidgetRemoved(msg.arg1)
                }
                HANDLE_PROVIDER_CHANGED -> {
                    onProviderChanged(msg.arg1, msg.obj as AppWidgetProviderInfo)
                }
                HANDLE_PROVIDERS_CHANGED -> {
                    onProvidersChanged()
                }
                HANDLE_VIEW_DATA_CHANGED -> {
                    viewDataChanged(msg.arg1, msg.arg2)
                }
            }
        }
    }

    private fun getListeners(appWidgetId: Int): List<AppWidgetHostView> {
        val listeners = ArrayList<AppWidgetHostView>()
        synchronized(mViews) {
            val items = mViews.entries.filter { it.key.appWidgetId == appWidgetId }.map {
                it.value
            }
            listeners.addAll(items)
        }
        return listeners
    }

    /**
     * Start receiving onAppWidgetChanged calls for your AppWidgets.  Call this when your activity
     * becomes visible, i.e. from onStart() in your Activity.
     */
    @Suppress("UNCHECKED_CAST")
    fun startListening() {
        val idsToUpdate: IntArray
        synchronized(mViews) {
            idsToUpdate = mViews.keys.map {
                it.appWidgetId
            }.toSet().toIntArray()
        }
        val updates: List<PendingHostUpdate>
        try {
            updates = sService.startListening(
                mCallbacks, mContextOpPackageName, mHostId, idsToUpdate
            ).list as List<PendingHostUpdate>
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
        val N = updates.size
        for (i in 0 until N) {
            val update = updates[i]
            when (update.type) {
                PendingHostUpdate.TYPE_VIEWS_UPDATE -> updateAppWidgetView(
                    update.appWidgetId,
                    update.views
                )
                PendingHostUpdate.TYPE_PROVIDER_CHANGED -> onProviderChanged(
                    update.appWidgetId,
                    update.widgetInfo
                )
                PendingHostUpdate.TYPE_VIEW_DATA_CHANGED -> viewDataChanged(
                    update.appWidgetId,
                    update.viewId
                )
                PendingHostUpdate.TYPE_APP_WIDGET_REMOVED -> dispatchOnAppWidgetRemoved(update.appWidgetId)
            }
        }
    }

    /**
     * Stop receiving onAppWidgetChanged calls for your AppWidgets.  Call this when your activity is
     * no longer visible, i.e. from onStop() in your Activity.
     */
    fun stopListening() {
        try {
            sService.stopListening(mContextOpPackageName, mHostId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
    }

    /**
     * Get a appWidgetId for a host in the calling process.
     *
     * @return a appWidgetId
     */
    fun allocateAppWidgetId(): Int {
        return try {
            sService.allocateAppWidgetId(mContextOpPackageName, mHostId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
    }

    /**
     * Starts an app widget provider configure activity for result on behalf of the caller.
     * Use this method if the provider is in another profile as you are not allowed to start
     * an activity in another profile. You can optionally provide a request code that is
     * returned in Activity#onActivityResult(int, int, android.content.Intent) and
     * an options bundle to be passed to the started activity.
     *
     *
     * Note that the provided app widget has to be bound for this method to work.
     *
     *
     * @param activity The activity from which to start the configure one.
     * @param appWidgetId The bound app widget whose provider's config activity to start.
     * @param requestCode Optional request code retuned with the result.
     * @param intentFlags Optional intent flags.
     *
     * @throws android.content.ActivityNotFoundException If the activity is not found.
     *
     * @see AppWidgetProviderInfo.getProfile
     */
    fun startAppWidgetConfigureActivityForResult(
        activity: Activity,
        appWidgetId: Int, intentFlags: Int, requestCode: Int, options: Bundle?
    ) {
        try {
            val intentSender = sService.createAppWidgetConfigIntentSender(
                mContextOpPackageName, appWidgetId, intentFlags
            )
            if (intentSender != null) {
                activity.startIntentSenderForResult(
                    intentSender, requestCode, null, 0, 0, 0,
                    options
                )
            } else {
                throw ActivityNotFoundException()
            }
        } catch (e: IntentSender.SendIntentException) {
            throw ActivityNotFoundException()
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
    }

    /**
     * Gets a list of all the appWidgetIds that are bound to the current host
     */
    val appWidgetIds: IntArray
        get() = try {
            sService.getAppWidgetIdsForHost(mContextOpPackageName, mHostId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }

    /**
     * Stop listening to changes for this AppWidget.
     */
    fun deleteAppWidgetId(appWidgetId: Int) {
        removeListener(appWidgetId)
        try {
            sService.deleteAppWidgetId(mContextOpPackageName, appWidgetId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
    }

    /**
     * Remove all records about this host from the AppWidget manager.
     *
     *  * Call this when initializing your database, as it might be because of a data wipe.
     *  * Call this to have the AppWidget manager release all resources associated with your
     * host.  Any future calls about this host will cause the records to be re-allocated.
     *
     */
    fun deleteHost() {
        try {
            sService.deleteHost(mContextOpPackageName, mHostId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
    }

    /**
     * Create the AppWidgetHostView for the given widget.
     * The AppWidgetHost retains a pointer to the newly-created View.
     */
    fun createView(
        context: Context,
        appWidgetId: Int,
        id: String = UUID.randomUUID().toString(),
        appWidget: AppWidgetProviderInfo?,
        interactionListener: SmartspaceTargetInteractionListener
    ): ExpandedAppWidgetHostView {
        val view = onCreateView(context, appWidgetId, appWidget) as ExpandedAppWidgetHostView
        view.setInteractionHandler(interactionListener)
        view.setAppWidget(appWidgetId, appWidget)
        view.id = addListener(appWidgetId, id, view)
        val views = try {
            sService.getAppWidgetViews(mContextOpPackageName, appWidgetId)
        } catch (e: RemoteException) {
            throw RuntimeException("system server dead?", e)
        }
        view.updateAppWidget(views)
        return view
    }

    /**
     *  Destroy a view, by removing its listener.
     */
    fun destroyView(view: ExpandedAppWidgetHostView) {
        val id = view.id ?: return
        removeListener(id)
    }

    /**
     * Called to create the AppWidgetHostView.  Override to return a custom subclass if you
     * need it.  {@more}
     */
    protected open fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return AppWidgetHostView(context)
    }

    /**
     * Called when the AppWidget provider for a AppWidget has been upgraded to a new apk.
     */
    protected fun onProviderChanged(appWidgetId: Int, appWidget: AppWidgetProviderInfo?) {
        val v = getListeners(appWidgetId)

        // Convert complex to dp -- we are getting the AppWidgetProviderInfo from the
        // AppWidgetService, which doesn't have our context, hence we need to do the
        // conversion here.
        Refine.unsafeCast<AppWidgetProviderInfoHidden>(appWidget).updateDimensions(mDisplayMetrics)
        v.forEach {
            it.resetAppWidget(appWidget)
        }
    }

    fun dispatchOnAppWidgetRemoved(appWidgetId: Int) {
        removeListener(appWidgetId)
        onAppWidgetRemoved(appWidgetId)
    }

    /**
     * Called when the app widget is removed for appWidgetId
     * @param appWidgetId
     */
    fun onAppWidgetRemoved(appWidgetId: Int) {
        // Does nothing
    }

    /**
     * Called when the set of available widgets changes (ie. widget containing packages
     * are added, updated or removed, or widget components are enabled or disabled.)
     */
    protected open fun onProvidersChanged() {
        // Does nothing
    }

    /**
     * Create an AppWidgetHostListener for the given widget.
     * The AppWidgetHost retains a pointer to the newly-created listener.
     * @param appWidgetId The ID of the app widget for which to add the listener
     * @param id The ID of the listener to add, which can be specified or random
     */
    private fun addListener(appWidgetId: Int, id: String, hostView: AppWidgetHostView): String {
        synchronized(mViews) {
            mViews.filter { it.key.appWidgetId == appWidgetId && it.key.id == id }.forEach {
                mViews.remove(it.key)
            }
            mViews.put(ListenerKey(appWidgetId, id), hostView)
        }
        return id
    }

    /**
     * Delete the listener for the given widget
     * @param appWidgetId The ID of the app widget for which the listener is to be deleted
     */
    private fun removeListener(appWidgetId: Int) {
        synchronized(mViews) {
            val keys = mViews.keys.filter { it.appWidgetId == appWidgetId }
            keys.forEach {
                mViews.remove(it)
            }
        }
    }

    /**
     * Delete the listener for the given ID
     * @param id The ID of the listener to be deleted
     */
    private fun removeListener(id: String) {
        synchronized(mViews) {
            val keys = mViews.keys.filter { it.id == id }
            keys.forEach {
                mViews.remove(it)
            }
        }
    }

    fun updateAppWidgetView(appWidgetId: Int, views: RemoteViews?) {
        val v = getListeners(appWidgetId)
        v.forEach {
            it.updateAppWidget(views)
        }
    }

    fun viewDataChanged(appWidgetId: Int, viewId: Int) {
        val v = getListeners(appWidgetId)
        v.forEach {
            it.viewDataChanged(viewId)
        }
    }

    fun getIntentSenderForConfigureActivity(appWidgetId: Int, intentFlags: Int = 0): IntentSender {
        return sService.createAppWidgetConfigIntentSender(
            BuildConfig.APPLICATION_ID, appWidgetId, intentFlags
        )
    }

    /**
     * Clear the list of Views that have been created by this AppWidgetHost.
     */
    protected fun clearViews() {
        synchronized(mViews) { mViews.clear() }
    }

    companion object {
        const val HANDLE_UPDATE = 1
        const val HANDLE_PROVIDER_CHANGED = 2
        const val HANDLE_PROVIDERS_CHANGED = 3
        const val HANDLE_VIEW_DATA_CHANGED = 4
        const val HANDLE_APP_WIDGET_REMOVED = 5

        var sService = bindService()

        private fun bindService(): IAppWidgetService {
            val b = ServiceManager.getService(Context.APPWIDGET_SERVICE)
            return IAppWidgetService.Stub.asInterface(b)
        }

        /**
         * Remove all records about all hosts for your package.
         *
         *  * Call this when initializing your database, as it might be because of a data wipe.
         *  * Call this to have the AppWidget manager release all resources associated with your
         * host.  Any future calls about this host will cause the records to be re-allocated.
         *
         */
        fun deleteAllHosts() {
            try {
                sService.deleteAllHosts()
            } catch (e: RemoteException) {
                throw RuntimeException("system server dead?", e)
            }
        }
    }

    class ListenerKey(val appWidgetId: Int, val id: String)


    /**
     * This interface specifies the actions to be performed on the app widget based on the calls
     * from the service
     */
    interface AppWidgetHostListener {
        /**
         * This function is called when the service want to reset the app widget provider info
         * @param appWidget The new app widget provider info
         */
        fun onUpdateProviderInfo(appWidget: AppWidgetProviderInfo?)

        /**
         * This function is called when the RemoteViews of the app widget is updated
         * @param views The new RemoteViews to be set for the app widget
         */
        fun updateAppWidget(views: RemoteViews?)

        /**
         * This function is called when the view ID is changed for the app widget
         * @param viewId The new view ID to be be set for the widget
         */
        fun onViewDataChanged(viewId: Int)
    }

}