package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.content.Intent
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.GlanceWidget
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.GoogleWeatherWidget
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.utils.extensions.getGoogleWeatherIntent
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class GoogleWeatherComplication: SmartspacerComplicationProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.complication.googleweather"
    }

    private val googleWeatherRepository by inject<GoogleWeatherRepository>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val state = googleWeatherRepository.getTodayState() ?: return emptyList()
        return listOf(
            ComplicationTemplate.Basic(
                id = "google_weather_at_${System.currentTimeMillis()}",
                icon = Icon(AndroidIcon.createWithBitmap(state.icon), shouldTint = false),
                content = Text(state.temperature),
                onClick = TapAction(
                    intent = provideContext().getGoogleWeatherIntent().apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                )
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = provideContext().getString(R.string.complication_google_weather_label),
            description = provideContext().getString(R.string.complication_google_weather_description),
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_complication_google_weather),
            compatibilityState = getCompatibility(),
            widgetProvider = GoogleWeatherWidget.AUTHORITY
        )
    }

    private fun getCompatibility(): CompatibilityState {
        return if(GlanceWidget.getProviderInfo() != null){
            CompatibilityState.Compatible
        }else{
            val unsupported = provideContext().getString(
                R.string.complication_google_weather_description_unsupported
            )
            CompatibilityState.Incompatible(unsupported)
        }
    }

}