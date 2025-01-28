package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget.TargetData.Padding
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.WidgetWidget
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.utils.extensions.checkCompatibility
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getBestRemoteViews
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class WidgetTarget: SmartspacerTargetProvider() {

    private val widgetRepository by inject<WidgetRepository>()
    private val dataRepository by inject<DataRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val remoteViews = widgetRepository.getWidgetTargetWidget(smartspacerId)
            ?: return emptyList()
        val data = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
        val padding = data?.padding ?: Padding.NONE
        val size = SizeF(
            widgetRepository.getDefaultWidth().toFloat(),
            padding.height.dp.toFloat()
        )
        val bestRemoteViews = remoteViews.getBestRemoteViews(provideContext(), size)
        val fallback = { title: Int, content: Int ->
            TargetTemplate.Basic(
                id = "widget_${smartspacerId}_at_${System.currentTimeMillis()}",
                componentName = ComponentName(provideContext(), WidgetTarget::class.java),
                title = Text(resources.getString(title)),
                subtitle = Text(resources.getString(content)),
                icon = Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_widgets))
            )
        }
        if(!remoteViews.checkCompatibility()) {
            return listOf(
                fallback(
                    R.string.target_widget_not_compatible_title,
                    R.string.target_widget_not_compatible_content
                ).create()
            )
        }
        val roundedRemoteViews = if(data?.rounded == true) {
            RemoteViews(
                provideContext().packageName, R.layout.remoteviews_wrapper_rounded
            ).apply {
                removeAllViews(R.id.remoteviews_rounded_wrapper)
                addView(R.id.remoteviews_rounded_wrapper, bestRemoteViews)
            }
        } else remoteViews
        val wrappedRemoteViews = RemoteViews(provideContext().packageName, padding.layout).apply {
            removeAllViews(R.id.remoteviews_padding_wrapper)
            addView(R.id.remoteviews_padding_wrapper, roundedRemoteViews)
        }
        return listOf(
            TargetTemplate.RemoteViews(
                wrappedRemoteViews,
                fallback(
                    R.string.target_widget_not_supported_title,
                    R.string.target_widget_not_supported_content
                )
            ).create()
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        val data = smartspacerId?.let { dataRepository.getTargetData(it, TargetData::class.java) }
        val description = if(data != null) {
            resources.getString(R.string.target_widget_description_set, data.label, data.appName)
        }else{
            resources.getText(R.string.target_widget_description)
        }
        return Config(
            label = resources.getString(R.string.target_widget_title),
            description = description,
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_widgets),
            allowAddingMoreThanOnce = true,
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), ConfigurationActivity.NavGraphMapping.TARGET_WIDGET
            ),
            setupActivity = ConfigurationActivity.createIntent(
                provideContext(), ConfigurationActivity.NavGraphMapping.TARGET_WIDGET_SETUP
            ),
            widgetProvider = WidgetWidget.AUTHORITY
        )
    }

    data class TargetData(
        @SerializedName("provider")
        val provider: String,
        @SerializedName("name")
        val label: String,
        @SerializedName("app_name")
        val appName: String,
        @SerializedName("rounded")
        val rounded: Boolean = true,
        @SerializedName("padding")
        val padding: Padding = Padding.NONE,
        @SerializedName("alt_sizing")
        val altSizing: Boolean? = false
    ) {
        val useAlternativeSizing
            get() = altSizing ?: false

        enum class Padding(@StringRes val label: Int, @LayoutRes val layout: Int, val height: Int) {
            NONE(R.string.target_widget_padding_none, R.layout.remoteviews_wrapper_padding_none, 96),
            SMALL(R.string.target_widget_padding_small, R.layout.remoteviews_wrapper_padding_small, 92),
            MEDIUM(R.string.target_widget_padding_medium, R.layout.remoteviews_wrapper_padding_medium, 88),
            LARGE(R.string.target_widget_padding_large, R.layout.remoteviews_wrapper_padding_large, 80),
            DISABLED(R.string.target_widget_padding_disable, R.layout.remoteviews_wrapper_padding_disabled, 96),
        }
    }

}