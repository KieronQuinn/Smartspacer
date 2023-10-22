package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.view.View
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import org.koin.java.KoinJavaComponent.get

abstract class GlanceWidget: SmartspacerWidgetProvider() {

    companion object {
        const val PACKAGE_NAME = "com.google.android.googlequicksearchbox"
        private const val IDENTIFIER_CONTENT = "$PACKAGE_NAME:id/assistant_smartspace_content"
        private const val IDENTIFIER_CONTENT_ALT = "$PACKAGE_NAME:id/smartspace_content"

        private val COMPONENT_AT_A_GLANCE = ComponentName(
            PACKAGE_NAME,
            "com.google.android.apps.gsa.staticplugins.smartspace.widget.SmartspaceWidgetProvider"
        )

        fun getProviderInfo(): AppWidgetProviderInfo? {
            val widgetRepository = get<WidgetRepository>(WidgetRepository::class.java)
            return widgetRepository.getProviders().firstOrNull {
                it.provider == COMPONENT_AT_A_GLANCE
            }?.apply {
                //Disable configuration activity as it's not required
                configure = null
            }
        }
    }

    private val provider by lazy {
        getProviderInfo()
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return provider
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config()
    }

    final override fun onWidgetChanged(
        smartspacerId: String,
        remoteViews: RemoteViews?
    ) {
        val views = remoteViews?.load() ?: return
        val result = if(views.isTNG()) {
            views.loadTNG(smartspacerId)
        }else{
            views.loadLegacy()
        }
        if(result) {
            notifyChange(smartspacerId)
        }
    }

    private fun View.isTNG(): Boolean {
        return (findViewByIdentifier<View>(IDENTIFIER_CONTENT)
            ?: findViewByIdentifier<View>(IDENTIFIER_CONTENT_ALT)) == null
    }

    abstract fun View.loadLegacy(): Boolean
    abstract fun View.loadTNG(smartspacerId: String): Boolean
    abstract fun notifyChange(smartspacerId: String)

}