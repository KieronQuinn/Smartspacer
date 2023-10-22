package com.kieronquinn.app.smartspacer.sdksample.plugin.utils

import android.content.Context
import com.kieronquinn.app.smartspacer.sdksample.plugin.service.StopwatchForegroundService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class Stopwatch {

    companion object {
        @JvmStatic
        private val INSTANCE = Stopwatch()

        fun getInstance() = INSTANCE
    }

    private val scope = MainScope()

    val stopwatchRunning = MutableStateFlow(false)

    val stopwatchValue = stopwatchRunning.flatMapLatest {
        if(it) tickerFlow(1_000L, 1_000L) else flowOf(0L)
    }.map { (it / 1000f).toLong() }.map {
        val day = TimeUnit.SECONDS.toDays(it).toInt()
        val hours = TimeUnit.SECONDS.toHours(it) - day * 24
        val minutes = TimeUnit.SECONDS.toMinutes(it) - TimeUnit.SECONDS.toHours(it) * 60
        val seconds = TimeUnit.SECONDS.toSeconds(it) - TimeUnit.SECONDS.toMinutes(it) * 60
        when {
            hours != 0L -> {
                "${hours.padWithZero()}:${minutes.padWithZero()}:${seconds.padWithZero()}"
            }
            minutes != 0L -> {
                "${minutes.padWithZero()}:${seconds.padWithZero()}"
            }
            else -> {
                "00:${seconds.padWithZero()}"
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, "00:00")

    private fun Long.padWithZero(): String {
        return toString().padStart(2, "0".toCharArray().first())
    }

    private fun tickerFlow(timePeriod: Long, initialDelay: Long) = flow {
        var time = 0L
        delay(initialDelay)
        while(true){
            time += timePeriod
            emit(time)
            delay(timePeriod)
        }
    }

    fun toggleStopwatch(context: Context) {
        scope.launch {
            val newState = !stopwatchRunning.value
            if(newState){
                //Start the service so the stopwatch can run in the background
                StopwatchForegroundService.startServiceIfNeeded(context)
            }
            stopwatchRunning.emit(newState)
        }
    }

}