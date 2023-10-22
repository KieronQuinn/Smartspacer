package com.kieronquinn.app.smartspacer.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class SmartspacerUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
): Worker(appContext, workerParams), KoinComponent {

    companion object {
        private const val UPDATE_CHECK_WORK_TAG = "smartspacer_update_check"
        private const val UPDATE_CHECK_HOUR = 12L

        private fun clearCheckWorker(context: Context){
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(UPDATE_CHECK_WORK_TAG)
        }

        fun queueCheckWorker(context: Context){
            clearCheckWorker(context)
            val checkWorker = Builder().build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                UPDATE_CHECK_WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, checkWorker
            )
        }
    }

    private val pluginRepository by inject<PluginRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val updateRepository by inject<UpdateRepository>()
    private val scope = MainScope()

    override fun doWork(): Result {
        if(isUpdateCheckEnabled()){
            checkUpdates()
        }
        if(isPluginUpdatesEnabled()){
            checkPluginUpdates()
        }
        return Result.success()
    }

    private fun isPluginUpdatesEnabled(): Boolean {
        if(!NotificationChannel.PLUGIN_UPDATES.isEnabled(applicationContext)) return false
        if(!settingsRepository.pluginRepositoryEnabled.getSync()) return false
        if(!settingsRepository.pluginRepositoryUpdateCheckEnabled.getSync()) return false
        return true
    }

    private fun isUpdateCheckEnabled(): Boolean {
        if(!NotificationChannel.UPDATES.isEnabled(applicationContext)) return false
        if(!settingsRepository.updateCheckEnabled.getSync()) return false
        return true
    }

    private fun checkPluginUpdates() = scope.launch {
        val updateCount = pluginRepository.getUpdateCount().first()
        if(updateCount == 0) return@launch
        val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("smartspacer://plugins")
            putExtra(MainActivity.EXTRA_SKIP_SPLASH, true)
        }
        notificationRepository.showNotification(
            NotificationId.PLUGIN_UPDATES,
            NotificationChannel.PLUGIN_UPDATES
        ) {
            it.setSmallIcon(R.drawable.ic_plugins)
            it.setContentTitle(
                applicationContext.getString(R.string.notification_plugin_updates_title)
            )
            it.setContentText(
                applicationContext.resources.getQuantityString(
                    R.plurals.notification_plugin_updates_content, updateCount, updateCount
                )
            )
            it.setContentIntent(
                PendingIntent.getActivity(
                applicationContext,
                NotificationId.PLUGIN_UPDATES.ordinal,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun checkUpdates() = scope.launch {
        val update = updateRepository.getUpdate() ?: return@launch
        val versionTo = update.versionName
        val versionFrom = BuildConfig.TAG_NAME
        notificationRepository.showNotification(
            NotificationId.UPDATES,
            NotificationChannel.UPDATES
        ) {
            it.setSmallIcon(R.drawable.ic_notification)
            it.setContentTitle(applicationContext.getString(R.string.update_notification_title))
            it.setContentText(
                applicationContext
                    .getString(R.string.update_notification_content, versionFrom, versionTo)
            )
            it.setContentIntent(
                PendingIntent.getActivity(
                applicationContext,
                NotificationId.UPDATES.ordinal,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    class Builder {
        fun build() : PeriodicWorkRequest {
            val delay = if (LocalDateTime.now().hour < UPDATE_CHECK_HOUR) {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            } else {
                Duration.between(ZonedDateTime.now(), ZonedDateTime.now().toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plusDays(1)
                    .plusHours(UPDATE_CHECK_HOUR)).toMinutes()
            }
            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            return PeriodicWorkRequest.Builder(
                SmartspacerUpdateWorker::class.java, 24, TimeUnit.HOURS
            ).addTag(UPDATE_CHECK_WORK_TAG)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(AlarmRepository.TAG_ALL)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                ).build()
        }
    }


}