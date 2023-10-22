package com.kieronquinn.app.smartspacer.components.smartspace.widgets

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GoogleWeatherComplication
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.TodayState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.findViewByIdentifier
import com.kieronquinn.app.smartspacer.sdk.utils.findViewsByType
import org.koin.android.ext.android.inject

class GoogleWeatherWidget: GlanceWidget() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widget.googleweather"
    }

    private val googleWeatherRepository by inject<GoogleWeatherRepository>()

    override fun View.loadLegacy(): Boolean {
        val todayState = loadLegacyAssistant() ?: loadLegacyNoAssistant() ?: return false
        googleWeatherRepository.setTodayState(todayState)
        return true
    }

    private fun View.loadLegacyAssistant(): TodayState? {
        val weatherIcon = findViewByIdentifier<ImageView>(
            "$PACKAGE_NAME:id/assistant_title_weather_icon"
        ) ?: findViewByIdentifier<ImageView>(
            "$PACKAGE_NAME:id/assistant_subtitle_weather_icon"
        ) ?: return null
        val weatherContent = findViewByIdentifier<TextView>(
            "$PACKAGE_NAME:id/assistant_title_weather_text"
        ) ?: findViewByIdentifier<TextView>(
            "$PACKAGE_NAME:id/assistant_subtitle_weather_text"
        ) ?: return null
        val content = weatherContent.text?.toString() ?: return null
        val icon = (weatherIcon.drawable as? BitmapDrawable)?.bitmap ?: return null
        return TodayState(icon, content)
    }

    private fun View.loadLegacyNoAssistant(): TodayState? {
        val weatherIcon = findViewByIdentifier<ImageView>(
            "$PACKAGE_NAME:id/title_weather_icon"
        ) ?: findViewByIdentifier<ImageView>(
            "$PACKAGE_NAME:id/subtitle_weather_icon"
        ) ?: return null
        val weatherContent = findViewByIdentifier<TextView>(
            "$PACKAGE_NAME:id/title_weather_text"
        ) ?: findViewByIdentifier<TextView>(
            "$PACKAGE_NAME:id/subtitle_weather_text"
        ) ?: return null
        val content = weatherContent.text?.toString() ?: return null
        val icon = (weatherIcon.drawable as? BitmapDrawable)?.bitmap ?: return null
        return TodayState(icon, content)
    }

    override fun View.loadTNG(smartspacerId: String): Boolean {
        this as android.view.ViewGroup
        val temperature = findViewsByType(TextView::class.java).lastOrNull {
            it.text.isNotBlank()
        } ?: return false
        val icon = findViewsByType(ImageView::class.java).lastOrNull {
            it.drawable is BitmapDrawable
        } ?: return false
        val state = TodayState(
            icon.drawable.toBitmap(),
            temperature.text.toString()
        )
        googleWeatherRepository.setTodayState(state)
        return true
    }

    override fun notifyChange(smartspacerId: String) {
        SmartspacerComplicationProvider.notifyChange(
            provideContext(), GoogleWeatherComplication::class.java, smartspacerId
        )
    }

}