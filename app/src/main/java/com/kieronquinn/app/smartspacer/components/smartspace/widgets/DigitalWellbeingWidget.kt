package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.appwidget.AppWidgetProviderInfo
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository.WellbeingState
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.smartspacer.utils.extensions.dip
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get

class DigitalWellbeingWidget: SmartspacerWidgetProvider() {

    companion object {
        const val PACKAGE_NAME = "com.google.android.apps.wellbeing"

        private const val COMPONENT_SCREEN_TIME =
            "com.google.android.apps.wellbeing.widget.screentime.ScreenTimeWidgetProviderReceiver_Receiver"

        private const val ID_TITLE = "com.google.android.apps.wellbeing:id/screen_time_title"
        private const val ID_SCREEN_TIME = "com.google.android.apps.wellbeing:id/total_screen_time"
        private const val ID_APP1_TIME = "com.google.android.apps.wellbeing:id/app1_time"
        private const val ID_APP1_NAME = "com.google.android.apps.wellbeing:id/app1_name"
        private const val ID_APP2_TIME = "com.google.android.apps.wellbeing:id/app2_time"
        private const val ID_APP2_NAME = "com.google.android.apps.wellbeing:id/app2_name"
        private const val ID_APP3_TIME = "com.google.android.apps.wellbeing:id/app3_time"
        private const val ID_APP3_NAME = "com.google.android.apps.wellbeing:id/app3_name"

        fun getProviderInfo(): AppWidgetProviderInfo? {
            val widgetRepository = get<WidgetRepository>(WidgetRepository::class.java)
            return widgetRepository.getProviders().firstOrNull {
                val provider = it.provider
                provider.packageName == PACKAGE_NAME && provider.className == COMPONENT_SCREEN_TIME
            }
        }
    }

    private val wellbeingRepository by inject<DigitalWellbeingRepository>()

    private val size by lazy {
        resources.dip(1000).toFloat()
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return getProviderInfo()
    }

    override fun onWidgetChanged(
        smartspacerId: String,
        remoteViews: RemoteViews?
    ) {
        val sizedViews = getSizedRemoteView(remoteViews ?: return, SizeF(size, size))
            ?: return
        val views = sizedViews.load() ?: return
        val titleTextView = views.findViewByIdentifier<TextView>(ID_TITLE) ?: return
        val screenTimeTextView = views.findViewByIdentifier<TextView>(ID_SCREEN_TIME) ?: return
        val app1NameTextView = views.findViewByIdentifier<TextView>(ID_APP1_NAME) ?: return
        val app1TimeTextView = views.findViewByIdentifier<TextView>(ID_APP1_TIME) ?: return
        val app2NameTextView = views.findViewByIdentifier<TextView>(ID_APP2_NAME) ?: return
        val app2TimeTextView = views.findViewByIdentifier<TextView>(ID_APP2_TIME) ?: return
        val app3NameTextView = views.findViewByIdentifier<TextView>(ID_APP3_NAME) ?: return
        val app3TimeTextView = views.findViewByIdentifier<TextView>(ID_APP3_TIME) ?: return
        val background = views.findViewById<View>(android.R.id.background)
        val clickIntent = background.getClickPendingIntent() ?: return
        val state = WellbeingState(
            titleTextView.text,
            screenTimeTextView.text,
            app1NameTextView.text,
            app1TimeTextView.text,
            app2NameTextView.text,
            app2TimeTextView.text,
            app3NameTextView.text,
            app3TimeTextView.text,
            clickIntent
        )
        wellbeingRepository.setState(state)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(size.toInt(), size.toInt())
    }

}