package com.kieronquinn.app.smartspacer.sdksample.plugin.utils

import android.content.Context
import android.os.CountDownTimer
import com.kieronquinn.app.smartspacer.sdksample.plugin.service.TimerForegroundService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Timer {

    companion object {
        @JvmStatic
        private val INSTANCE = Timer()

        fun getInstance() = INSTANCE
    }

    private val scope = MainScope()

    private val countdownTimer = callbackFlow {
        val timer = object: CountDownTimer(30_000L, 1_000L) {
            override fun onTick(time: Long) {
                trySend(time)
            }

            override fun onFinish() {
                trySend(0L)
                scope.launch {
                    timerRunning.emit(false)
                }
            }
        }
        timer.start()
        awaitClose {
            timer.cancel()
        }
    }

    val timerRunning = MutableStateFlow(false)

    val timerValue = timerRunning.flatMapLatest {
        if(it) countdownTimer else flowOf(30_000L)
    }.map {
        val seconds = (it / 1000.0).format(0).padStart(2, "0".toCharArray().first())
        "0:$seconds"
    }.stateIn(scope, SharingStarted.Eagerly, "00:30")

    fun toggleTimer(context: Context) {
        scope.launch {
            val newState = !timerRunning.value
            if(newState){
                //Start the service so the timer can run in the background
                TimerForegroundService.startServiceIfNeeded(context)
            }
            timerRunning.emit(newState)
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

}