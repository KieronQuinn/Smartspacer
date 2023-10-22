package com.kieronquinn.app.smartspacer.sdksample.plugin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.targets.TimerTarget
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Timer
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.extensions.startForegroundCompat
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.whenCreated

class TimerForegroundService: LifecycleService() {

    companion object {
        fun startServiceIfNeeded(context: Context){
            context.startForegroundService(Intent(context, TimerForegroundService::class.java))
        }
    }

    private val timer = Timer.getInstance()

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat(1, createNotification())
        setupState()
        setupTimer()
    }

    private fun setupState() {
        whenCreated {
            timer.timerRunning.collect {
                if(!it) stopSelf()
            }
        }
    }

    private fun setupTimer() {
        handleStopwatchChange()
        whenCreated {
            timer.timerValue.collect {
                handleStopwatchChange()
            }
        }
    }

    private fun handleStopwatchChange() {
        SmartspacerTargetProvider.notifyChange(this, TimerTarget::class.java)
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val channel = NotificationChannel(
            "timer", "Timer Service", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, "timer").apply {
            setSmallIcon(R.drawable.ic_target_timer_stopwatch)
            setContentTitle("Timer Running")
            setContentText("Example timer is running")
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOngoing(true)
        }.build().also {
            notificationManager.notify(1, it)
        }
    }

}