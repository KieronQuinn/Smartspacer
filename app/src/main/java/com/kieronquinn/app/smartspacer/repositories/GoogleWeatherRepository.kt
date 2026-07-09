package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.TodayState
import kotlinx.coroutines.flow.StateFlow

interface GoogleWeatherRepository {

    val todayStateFlow: StateFlow<TodayState?>

    fun getTodayState(): TodayState?
    fun setTodayState(todayState: TodayState)

    fun getForecastState(): ForecastState?
    fun setForecastState(forecastState: ForecastState)

    data class TodayState(
        val icon: Bitmap,
        val temperature: String
    )

    data class ForecastState(
        val location: String,
        val condition: String,
        val now: ForecastItem,
        val forecast: List<ForecastItem>,
        val clickIntent: PendingIntent?
    ) {
        data class ForecastItem(
            val time: String?,
            val temperature: String,
            val icon: Bitmap
        )
    }

}

class GoogleWeatherRepositoryImpl: GoogleWeatherRepository {

    private val _todayStateFlow = kotlinx.coroutines.flow.MutableStateFlow<TodayState?>(null)
    override val todayStateFlow: StateFlow<TodayState?> = _todayStateFlow

    private var forecastState: ForecastState? = null

    override fun getTodayState(): TodayState? {
        return _todayStateFlow.value
    }

    override fun setTodayState(todayState: TodayState) {
        _todayStateFlow.value = todayState
    }

    override fun getForecastState(): ForecastState? {
        return forecastState
    }

    override fun setForecastState(forecastState: ForecastState) {
        this.forecastState = forecastState
    }

}