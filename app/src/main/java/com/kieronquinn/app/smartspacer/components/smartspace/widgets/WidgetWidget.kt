package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import org.koin.android.ext.android.inject

class WidgetWidget: SmartspacerWidgetProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widget.widget"
    }

    private val widgetRepository by inject<WidgetRepository>()
    private val dataRepository by inject<DataRepository>()

    override fun onWidgetChanged(smartspacerId: String, remoteViews: RemoteViews?) {
        widgetRepository.setWidgetTargetWidget(smartspacerId, remoteViews ?: return)
    }

    override fun getConfig(smartspacerId: String): Config {
        val widget = dataRepository.getTargetData(smartspacerId, WidgetTarget.TargetData::class.java)
            ?: return Config()
        return Config(
            height = widget.padding.height.dp
        )
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        val widget = dataRepository.getTargetData(smartspacerId, WidgetTarget.TargetData::class.java)
            ?.let { ComponentName.unflattenFromString(it.provider) } ?: return null
        return widgetRepository.getProviders().firstOrNull { it.provider == widget }
    }

}