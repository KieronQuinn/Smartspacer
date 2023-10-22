package com.kieronquinn.app.smartspacer.providers

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.smartspace.Widget
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.BaseProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_APP_WIDGET_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_INTENT
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_INTENT_SENDER
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_PENDING_INTENT
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_REMOTE_VIEWS
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_SIZE_F
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_VIEW_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_VIEW_IDENTIFIER
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_CLICK_VIEW
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_GET_APP_WIDGET_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_GET_RECONFIGURE_INTENT_SENDER
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_GET_SIZED_REMOTE_VIEWS
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_LAUNCH_FILL_IN_INTENT
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getResourceForIdentifier
import com.kieronquinn.app.smartspacer.utils.extensions.createPackageContextOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageName
import com.kieronquinn.app.smartspacer.utils.extensions.getRemoteViewsToApply
import com.kieronquinn.app.smartspacer.utils.extensions.removeActionsForId
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 *  Handles calls from plugin widgets to communicate with the app widget host
 */
class SmartspacerWidgetProvider: BaseProvider() {

    private val widgetRepository by inject<WidgetRepository>()
    private val scope = MainScope()

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when(method){
            METHOD_GET_RECONFIGURE_INTENT_SENDER -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                if(!verifySecurityBlocking(smartspacerId, callingPackage)) return null
                bundleOf(EXTRA_INTENT_SENDER to getIntentSender(smartspacerId))
            }
            METHOD_GET_APP_WIDGET_ID -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                if(!verifySecurityBlocking(smartspacerId, callingPackage)) return null
                return bundleOf(
                    EXTRA_APP_WIDGET_ID to getAppWidgetIdForSmartspacerId(smartspacerId)
                )
            }
            METHOD_CLICK_VIEW -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val identifier = extras.getString(EXTRA_VIEW_IDENTIFIER)
                val viewId = extras.getInt(EXTRA_VIEW_ID, -1).takeIf { it > 0 }
                if(identifier == null && viewId == null) return null
                val callingPackage = callingPackage
                //Avoid blocking main thread on app launch
                scope.launch {
                    if(!verifySecurity(smartspacerId, callingPackage)) return@launch
                    val widget = getWidgetForSmartspacerId(smartspacerId) ?: return@launch
                    widgetRepository.clickAppWidgetIdView(widget.appWidgetId, identifier, viewId)
                }
                null
            }
            METHOD_LAUNCH_FILL_IN_INTENT -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val intent = extras.getParcelableCompat(EXTRA_INTENT, Intent::class.java)
                    ?: return null
                val pendingIntent = extras.getParcelableCompat(
                    EXTRA_PENDING_INTENT, PendingIntent::class.java
                ) ?: return null
                //Avoid blocking main thread on app launch
                scope.launch {
                    if(!verifySecurity(smartspacerId, callingPackage)) return@launch
                    widgetRepository.launchFillInIntent(pendingIntent, intent)
                }
                null
            }
            METHOD_GET_SIZED_REMOTE_VIEWS -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
                val remoteViews = extras?.getParcelableCompat(
                    EXTRA_REMOTE_VIEWS, RemoteViews::class.java
                ) ?: return null
                val sizeF = extras.getParcelableCompat(EXTRA_SIZE_F, SizeF::class.java)
                    ?: return null
                val sizedRemoteViews = remoteViews.getRemoteViewsToApply(provideContext(), sizeF)
                bundleOf(EXTRA_REMOTE_VIEWS to RemoteViews(sizedRemoteViews))
            }
            SmartspacerWidgetProvider.METHOD_REMOVE_ACTIONS -> {
                val remoteViews = extras?.getParcelableCompat(
                    EXTRA_REMOTE_VIEWS, RemoteViews::class.java
                ) ?: return null
                val identifier = extras.getString(EXTRA_VIEW_IDENTIFIER)
                val viewId = extras.getInt(EXTRA_VIEW_ID, -1).takeIf { it > 0 }
                if(identifier == null && viewId == null) return null
                val packageName = remoteViews.getPackageName()
                val widgetContext = provideContext().createPackageContextOrNull(packageName)
                    ?: return null
                val id = viewId ?: identifier?.let { widgetContext.getResourceForIdentifier(it) }
                    ?: return null
                remoteViews.removeActionsForId(id)
                bundleOf(EXTRA_REMOTE_VIEWS to remoteViews.copy())
            }
            SmartspacerWidgetProvider.METHOD_GET_ADAPTER -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val identifier = extras.getString(EXTRA_VIEW_IDENTIFIER)
                val id = extras.getInt(EXTRA_VIEW_ID, -1).takeIf { it > 0 }
                if(identifier == null && id == null) return null
                val callingPackage = callingPackage
                //Avoid blocking main thread on app launch
                scope.launch {
                    if(!verifySecurity(smartspacerId, callingPackage)) return@launch
                    val widget = getWidgetForSmartspacerId(smartspacerId) ?: return@launch
                    widgetRepository.loadAdapter(smartspacerId, widget.appWidgetId, identifier, id)
                }
                null
            }
            SmartspacerWidgetProvider.METHOD_GET_REMOTE_COLLECTION_ITEMS -> {
                //Requires S+
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                val identifier = extras.getString(EXTRA_VIEW_IDENTIFIER)
                val id = extras.getInt(EXTRA_VIEW_ID, -1).takeIf { it > 0 }
                if(identifier == null && id == null) return null
                val callingPackage = callingPackage
                //Avoid blocking main thread on app launch
                scope.launch {
                    if(!verifySecurity(smartspacerId, callingPackage)) return@launch
                    val widget = getWidgetForSmartspacerId(smartspacerId) ?: return@launch
                    widgetRepository.loadRemoteCollectionItems(
                        smartspacerId,
                        widget.appWidgetId,
                        identifier,
                        id
                    )
                }
                null
            }
            else -> null
        }
    }

    private fun verifySecurityBlocking(
        smartspacerId: String,
        callingPackage: String?
    ): Boolean = runBlocking {
        verifySecurity(smartspacerId, callingPackage)
    }

    private suspend fun verifySecurity(smartspacerId: String, callingPackage: String?): Boolean {
        val callingPackageOrSelf = callingPackage ?: BuildConfig.APPLICATION_ID
        val widget = getWidgetForSmartspacerId(smartspacerId) ?: return false
        return widget.sourcePackage == callingPackageOrSelf
    }

    private fun getAppWidgetIdForSmartspacerId(smartspacerId: String): Int? {
        return getWidgetForSmartspacerIdBlocking(smartspacerId)?.appWidgetId
    }

    private fun getWidgetForSmartspacerIdBlocking(smartspacerId: String) = runBlocking {
        getWidgetForSmartspacerId(smartspacerId)
    }

    private suspend fun getWidgetForSmartspacerId(smartspacerId: String): Widget? {
        return widgetRepository.widgets.firstNotNull().firstOrNull {
            it.id == smartspacerId
        }
    }

    private fun getIntentSender(smartspacerId: String): IntentSender? {
        val appWidgetId = getAppWidgetIdForSmartspacerId(smartspacerId) ?: return null
        return widgetRepository.createConfigIntentSender(appWidgetId)
    }

}