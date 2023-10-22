package com.kieronquinn.app.smartspacer.repositories

import android.graphics.Bitmap
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.ForecastState
import com.kieronquinn.app.smartspacer.repositories.GoogleWeatherRepository.TodayState

interface GoogleWeatherRepository {

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
        val forecast: List<ForecastItem>
    ) {
        data class ForecastItem(
            val time: String?,
            val temperature: String,
            val icon: Bitmap
        )
    }

}

class GoogleWeatherRepositoryImpl: GoogleWeatherRepository {

    private var todayState: TodayState? = null
    private var forecastState: ForecastState? = null

    override fun getTodayState(): TodayState? {
        return todayState
    }

    override fun setTodayState(todayState: TodayState) {
        this.todayState = todayState
    }

    override fun getForecastState(): ForecastState? {
        return forecastState
    }

    override fun setForecastState(forecastState: ForecastState) {
        this.forecastState = forecastState
    }

}