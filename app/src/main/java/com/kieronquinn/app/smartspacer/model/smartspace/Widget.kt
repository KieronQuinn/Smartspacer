package com.kieronquinn.app.smartspacer.model.smartspace

import android.appwidget.AppWidgetProviderInfo
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_REMOTE_ADAPTER
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_REMOTE_VIEWS
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_VIEW_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.EXTRA_VIEW_IDENTIFIER
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_GET_CONFIG
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_ON_REMOVED
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Companion.METHOD_ON_WIDGET_CHANGED
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider.Config
import com.kieronquinn.app.smartspacer.sdk.utils.RemoteAdapter
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat
import com.kieronquinn.app.smartspacer.utils.extensions.callSafely
import com.kieronquinn.app.smartspacer.utils.extensions.getResourceNameOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.observerAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable
import com.kieronquinn.app.smartspacer.model.database.Widget as DatabaseWidget

class Widget(
    val context: Context,
    val appWidgetId: Int,
    val info: AppWidgetProviderInfo,
    val authority: String,
    val id: String,
    val type: DatabaseWidget.Type,
    val sourcePackage: String = BuildConfig.APPLICATION_ID
): KoinComponent, Closeable {

    companion object {
        fun getAppWidgetProviderInfo(
            contentResolver: ContentResolver, authority: String, id: String
        ): AppWidgetProviderInfo? {
            val uri = Uri.Builder()
                .scheme("content")
                .authority(authority)
                .build()
            val bundle = contentResolver.callSafely(
                uri,
                SmartspacerWidgetProvider.METHOD_GET_INFO,
                null,
                bundleOf(
                    EXTRA_SMARTSPACER_ID to id
                )
            )
            return bundle?.getParcelableCompat(
                SmartspacerWidgetProvider.EXTRA_APP_WIDGET_PROVIDER_INFO,
                AppWidgetProviderInfo::class.java
            )
        }

        fun getConfig(contentResolver: ContentResolver, authority: String, id: String): Config? {
            val uri = Uri.Builder()
                .scheme("content")
                .authority(authority)
                .build()
            val config = contentResolver.callSafely(
                uri,
                METHOD_GET_CONFIG,
                null,
                bundleOf(EXTRA_SMARTSPACER_ID to id)
            ) ?: return null
            return Config(config)
        }
    }

    private val idBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .appendPath(id)
        .build()

    private val authorityBasedUri = Uri.Builder()
        .scheme("content")
        .authority(authority)
        .build()

    private val packageRepository by inject<PackageRepository>()
    private val contentResolver = context.contentResolver
    private val scope = MainScope()
    private val widgetRepository by inject<WidgetRepository>()

    private val appChange = packageRepository.onPackageChanged(scope, sourcePackage)

    private val authorityBasedRemoteChange = contentResolver.observerAsFlow(authorityBasedUri)
    private val idBasedRemoteChange = contentResolver.observerAsFlow(idBasedUri)

    private val change = combine(appChange, authorityBasedRemoteChange, idBasedRemoteChange) { _, _, _ ->
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val remoteConfig = change.map {
        getRemoteConfig()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun getPluginConfig() = remoteConfig

    private suspend fun getRemoteConfig() = withContext(Dispatchers.IO) {
        val config = callRemote(
            METHOD_GET_CONFIG,
            bundleOf(EXTRA_SMARTSPACER_ID to id)
        ) ?: return@withContext null
        if(config.isEmpty) return@withContext null
        return@withContext Config(config)
    }

    suspend fun onWidgetChanged(remoteViews: RemoteViews?) {
        callRemote(
            METHOD_ON_WIDGET_CHANGED,
            bundleOf(
                EXTRA_SMARTSPACER_ID to id,
                EXTRA_REMOTE_VIEWS to remoteViews?.copy()
            )
        )
    }

    suspend fun onViewDataChanged(viewId: Int) {
        val context = createContext() ?: return
        val viewIdentifier = context.getIdentifier(viewId)
        callRemote(
            SmartspacerWidgetProvider.METHOD_ON_VIEW_DATA_CHANGED,
            bundleOf(
                EXTRA_SMARTSPACER_ID to id,
                EXTRA_VIEW_IDENTIFIER to viewIdentifier,
                EXTRA_VIEW_ID to viewId
            )
        )
    }

    suspend fun onAdapterLoaded(adapter: RemoteAdapter) {
        callRemote(
            SmartspacerWidgetProvider.METHOD_ON_ADAPTER_CONNECTED,
            bundleOf(
                EXTRA_SMARTSPACER_ID to id,
                EXTRA_REMOTE_ADAPTER to adapter.toBundle()
            )
        )
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

    override fun close() {
        scope.cancel()
    }

    suspend fun onDeleted() {
        callRemote(
            METHOD_ON_REMOVED,
            bundleOf(
                EXTRA_SMARTSPACER_ID to id
            )
        )
        widgetRepository.deallocateAppWidgetId(appWidgetId)
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    private fun createContext(): Context? {
        return try {
            context.createPackageContext(info.provider.packageName, Context.CONTEXT_IGNORE_SECURITY)
        }catch (e: NameNotFoundException) {
            null
        }
    }

    private fun Context.getIdentifier(id: Int): String? {
        return resources.getResourceNameOrNull(id)
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + appWidgetId
        result = 31 * result + info.hashCode()
        result = 31 * result + authority.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + sourcePackage.hashCode()
        return result
    }

}