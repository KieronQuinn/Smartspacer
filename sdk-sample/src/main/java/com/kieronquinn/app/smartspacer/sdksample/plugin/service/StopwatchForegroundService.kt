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
import com.kieronquinn.app.smartspacer.sdksample.plugin.targets.StopwatchTarget
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Stopwatch
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.extensions.startForegroundCompat
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.whenCreated

class StopwatchForegroundService: LifecycleService() {

    companion object {
        fun startServiceIfNeeded(context: Context){
            context.startForegroundService(Intent(context, StopwatchForegroundService::class.java))
        }
    }

    private val stopwatch = Stopwatch.getInstance()

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat(2, createNotification())
        setupState()
        setupTimer()
    }

    private fun setupState() {
        whenCreated {
            stopwatch.stopwatchRunning.collect {
                if(!it) stopSelf()
            }
        }
    }

    private fun setupTimer() {
        handleTimerChange()
        whenCreated {
            stopwatch.stopwatchValue.collect {
                handleTimerChange()
            }
        }
    }

    private fun handleTimerChange() {
        SmartspacerTargetProvider.notifyChange(this, StopwatchTarget::class.java)
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val channel = NotificationChannel(
            "stopwatch", "Stopwatch Service", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, "stopwatch").apply {
            setSmallIcon(R.drawable.ic_target_timer_stopwatch)
            setContentTitle("Stopwatch Running")
            setContentText("Example stopwatch is running")
            priority = NotificationCompat.PRIORITY_DEFAULT
            setOngoing(true)
        }.build().also {
            notificationManager.notify(2, it)
        }
    }

}