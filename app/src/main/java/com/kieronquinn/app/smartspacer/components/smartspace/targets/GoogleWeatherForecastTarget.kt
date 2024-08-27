package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.os.Build
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.widgets.GoogleWeatherForecastWidget.Companion.getProviderInfo
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState.ForecastItem
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData.CarouselItem
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.utils.extensions.getGoogleWeatherIntent
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class GoogleWeatherForecastTarget: SmartspacerTargetProvider() {

    private val googleWeatherRepository by inject<GoogleWeatherRepository>()
    private val widgetRepository by inject<WidgetRepository>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val state = googleWeatherRepository.getForecastState() ?: return emptyList()
        return listOf(state.toTarget(smartspacerId))
    }

    private fun ForecastState.toTarget(smartpacerId: String): SmartspaceTarget {
        val tapAction = clickIntent?.let { TapAction(pendingIntent = it) } ?: getTapAction()
        return TargetTemplate.Carousel(
            id = "google_weather_forecast_$smartpacerId",
            componentName = ComponentName(provideContext(), this::class.java),
            title = Text(location),
            subtitle = Text("${now.temperature} $condition"),
            icon = Icon(AndroidIcon.createWithBitmap(now.icon), shouldTint = false),
            items = forecast.toCarouselItems(tapAction),
            onClick = tapAction,
            onCarouselClick = tapAction,
            subComplication = ComplicationTemplate.blank().create()
        ).create().apply {
            canBeDismissed = false
        }
    }

    private fun List<ForecastItem>.toCarouselItems(tapAction: TapAction): List<CarouselItem> {
        return map {
            CarouselItem(
                Text(it.temperature),
                Text(" ${it.time} "),
                Icon(AndroidIcon.createWithBitmap(it.icon), shouldTint = false),
                tapAction
            )
        }
    }

    private fun getTapAction(): TapAction {
        return TapAction(
            intent = provideContext().getGoogleWeatherIntent()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.target_google_weather_forecast_title),
            resources.getString(R.string.target_google_weather_forecast_content),
            AndroidIcon.createWithResource(
                provideContext(), R.drawable.ic_complication_google_weather
            ),
            widgetProvider = "${BuildConfig.APPLICATION_ID}.widget.googleweatherforecast",
            compatibilityState = getCompatibilityState(),
            allowAddingMoreThanOnce = true
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    private fun getCompatibilityState(): CompatibilityState {
        return if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            || getProviderInfo(widgetRepository) == null){
            CompatibilityState.Incompatible(
                resources.getString(R.string.target_google_weather_forecast_incompatible)
            )
        }else CompatibilityState.Compatible
    }

}