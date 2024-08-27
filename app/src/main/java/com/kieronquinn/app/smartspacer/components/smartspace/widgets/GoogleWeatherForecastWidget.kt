package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.SizeF
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.graphics.scale
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GoogleWeatherForecastTarget
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState.ForecastItem
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import org.koin.android.ext.android.inject

class GoogleWeatherForecastWidget: SmartspacerWidgetProvider() {

    companion object {
        private const val PACKAGE_NAME = "com.google.android.googlequicksearchbox"
        private const val CLASS =
            "com.google.android.apps.search.assistant.verticals.snapshot.widgets.weather.WeatherWidget_Receiver"

        private const val IDENTIFIER_LOCATION =
            "$PACKAGE_NAME:id/assistant_weather_widget_location_title"
        private const val IDENTIFIER_CONDITION =
            "$PACKAGE_NAME:id/assistant_weather_widget_condition_title"
        private const val IDENTIFIER_ICON =
            "$PACKAGE_NAME:id/assistant_weather_icon"
        private const val IDENTIFIER_TEMPERATURE =
            "$PACKAGE_NAME:id/assistant_weather_widget_temperature"
        private const val IDENTIFIER_FORECAST_TEMPERATURE =
            "$PACKAGE_NAME:id/assistant_weather_widget_forecast_temperature_"
        private const val IDENTIFIER_FORECAST_ICON =
            "$PACKAGE_NAME:id/assistant_weather_widget_forecast_icon_"
        private const val IDENTIFIER_FORECAST_TIME =
            "$PACKAGE_NAME:id/assistant_weather_widget_forecast_hour_"

        private val SMALL_ICON_SIZE = 24.dp
        private val WIDGET_WIDTH = 368.dp
        private val WIDGET_HEIGHT = 146.dp

        fun getProviderInfo(widgetRepository: WidgetRepository): AppWidgetProviderInfo? {
            return widgetRepository.getProviders().firstOrNull {
                it.provider.packageName == PACKAGE_NAME && it.provider.className == CLASS
            }
        }
    }

    private val widgetRepository by inject<WidgetRepository>()
    private val googleWeatherRepository by inject<GoogleWeatherRepository>()

    override fun onWidgetChanged(smartspacerId: String, remoteViews: RemoteViews?) {
        val fullSize = SizeF(WIDGET_WIDTH.toFloat(), WIDGET_HEIGHT.toFloat())
        val fullWidthRemoteViews = getSizedRemoteView(remoteViews ?: return, fullSize)
        val views = fullWidthRemoteViews?.load() ?: return
        val location = views.findViewByIdentifier<TextView>(IDENTIFIER_LOCATION)?.text?.toString()
            ?: return
        val condition = views.findViewByIdentifier<TextView>(IDENTIFIER_CONDITION)?.text?.toString()
            ?: return
        val icon = views.findViewByIdentifier<ImageView>(IDENTIFIER_ICON)
            ?.getImageAsBitmap() ?: return
        val temperature = views.findViewByIdentifier<TextView>(IDENTIFIER_TEMPERATURE)?.text
            ?.toString() ?: return
        val forecasts = ArrayList<ForecastItem>()
        for(i in 1 until 5) {
            val forecastTime = views.findViewByIdentifier<TextView>(
                IDENTIFIER_FORECAST_TIME + i
            )?.text?.toString() ?: continue
            val forecastTemperature = views.findViewByIdentifier<TextView>(
                IDENTIFIER_FORECAST_TEMPERATURE + i
            )?.text?.toString() ?: continue
            val forecastIcon = views.findViewByIdentifier<ImageView>(
                IDENTIFIER_FORECAST_ICON + i
            )?.getImageAsBitmap()?.scale() ?: continue
            forecasts.add(ForecastItem(forecastTime, forecastTemperature, forecastIcon))
        }
        val clickIntent = views.findViewById<View>(android.R.id.background)
            ?.getClickPendingIntent()
        val forecastState = ForecastState(
            location, condition, ForecastItem(null, temperature, icon), forecasts, clickIntent
        )
        googleWeatherRepository.setForecastState(forecastState)
        SmartspacerTargetProvider.notifyChange(
            provideContext(), GoogleWeatherForecastTarget::class.java
        )
    }

    private fun Bitmap.scale(): Bitmap {
        return scale(SMALL_ICON_SIZE, SMALL_ICON_SIZE)
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return getProviderInfo(widgetRepository)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(WIDGET_WIDTH, WIDGET_HEIGHT)
    }

    private fun ImageView.getImageAsBitmap(): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
    }

}