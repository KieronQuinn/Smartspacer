package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.complications.AlarmComplication.Companion.getNextAlarmChangedTime
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.TimeDateRequirement.TimeDateRequirementData
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.receivers.AlarmComplicationAlarmReceiver
import com.kieronquinn.app.smartspacer.receivers.CalendarTargetAlarmReceiver
import com.kieronquinn.app.smartspacer.receivers.CalendarTargetTOTMAlarmReceiver
import com.kieronquinn.app.smartspacer.receivers.DailyUpdateAlarmReceiver
import com.kieronquinn.app.smartspacer.receivers.GreetingTargetAlarmReceiver
import com.kieronquinn.app.smartspacer.receivers.TimeDateRequirementAlarmReceiver
import com.kieronquinn.app.smartspacer.utils.extensions.atStartOfMinute
import com.kieronquinn.app.smartspacer.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.smartspacer.utils.extensions.getIgnoreBatteryOptimisationsIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

interface AlarmRepository {

    companion object {
        const val MAX_SCHEDULER_LIMIT = 50
        const val TAG_ALL = "all"
    }

    /**
     *  Returns whether the device can schedule exact alarms and keep them running. This is
     *  currently just checking battery optimisation is disabled.
     */
    fun canScheduleExactAlarm(): Boolean

    /**
     *  Enqueues the next time date requirement alarm, to be called from a fresh state or the last
     *  alarm triggering.
     */
    fun enqueueNextTimeDateRequirementReceiver()

    /**
     *  Enqueues the next Calendar Target refresh to be called when an event Target will be shown
     */
    fun enqueueNextCalendarTargetReceiver()

    /**
     *  Enqueues the Calendar Target's top-of-the-minute receiver. This is a duplication of
     *  Smartspace as the Smartspace update does not always happen at the start of the minute,
     *  which is important for minute-level countdowns
     */
    fun enqueueNextCalendarTargetTOTMReceiver()

    /**
     *  Enqueues the next Greeting Target refresh time to change the greeting text
     */
    fun enqueueNextGreetingTargetReceiver()

    /**
     *  Enqueues the Alarm Complication's receiver for when it will have changed, ie. 12h before
     *  the alarm itself is due to go off
     */
    fun enqueueNextAlarmChangedReceiver()

    /**
     *  Enqueues the next daily update receiver, at midnight. This reloads calendar events and
     *  refreshes the date target
     */
    fun enqueueDailyUpdateReceiver()

    /**
     *  Triggered when a requirement such as the timezone or device booting changes. Calls enqueue
     *  on all the alarm types.
     */
    fun onRequirementChanged()

}

@SuppressLint("MissingPermission", "ScheduleExactAlarm", "BatteryLife")
class AlarmRepositoryImpl(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val calendarRepository: CalendarRepository,
    dataRepository: DataRepository,
    private val scope: CoroutineScope = MainScope()
): AlarmRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    @VisibleForTesting
    val scheduleTimeDateRequirementAlarmBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val scheduleCalendarTargetAlarmBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val scheduleAlarmComplicationAlarmBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val scheduleCalendarTargetTOTMAlarmBus = MutableSharedFlow<Unit>()

    @VisibleForTesting
    val scheduleGreetingTargetAlarmBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val timeDateRequirements = dataRepository.getRequirementData(
        RequirementDataType.TIME_DATE, TimeDateRequirementData::class.java
    ).flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    @VisibleForTesting
    fun setupBatteryOptimisationChange() = scope.launch {
        context.broadcastReceiverAsFlow(
            IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED")
        ).map {
            canScheduleExactAlarm()
        }.collect {
            enqueueNextTimeDateRequirementReceiver()
            enqueueNextCalendarTargetReceiver()
            enqueueNextGreetingTargetReceiver()
            enqueueDailyUpdateReceiver()
        }
    }

    @VisibleForTesting
    fun setupTimeDateRequirementAlarms() = scope.launch {
        scheduleTimeDateRequirementAlarmBus.flatMapLatest {
            getNextTimeDateRequirements()
        }.collect { nextTimeDateRequirements ->
            val ids = nextTimeDateRequirements?.second?.map { it.id }?.toTypedArray()
                ?: return@collect
            if(!canScheduleExactAlarm()) {
                showBatteryOptimisationNotification()
                return@collect
            }else{
                notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
            }
            val intent = TimeDateRequirementAlarmReceiver.createIntent(context, ids)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                nextTimeDateRequirements.first,
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.TIME_DATE_ALARM.ordinal,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    private fun setupCalendarTargetAlarms() = scope.launch {
        scheduleCalendarTargetAlarmBus.mapNotNull {
            calendarRepository.getNextCalendarTrigger()
        }.collect {
            if(!canScheduleExactAlarm()) {
                showBatteryOptimisationNotification()
                return@collect
            }else{
                notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
            }
            val intent = CalendarTargetAlarmReceiver.createIntent(context)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                it.time.toEpochMilli(),
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.CALENDAR_ALARM.ordinal,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    private fun setupCalendarTargetTOTMAlarms() = scope.launch {
        scheduleCalendarTargetTOTMAlarmBus.mapNotNull {
            ZonedDateTime.now().plusMinutes(1).atStartOfMinute()
        }.collect {
            if(!canScheduleExactAlarm()) {
                showBatteryOptimisationNotification()
                return@collect
            }else{
                notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
            }
            val intent = CalendarTargetTOTMAlarmReceiver.createIntent(context)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                it.toInstant().toEpochMilli(),
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.CALENDAR_TOTM_ALARM.ordinal,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    private fun setupGreetingTargetAlarms() = scope.launch {
        scheduleGreetingTargetAlarmBus.mapNotNull {
            GreetingTarget.getNextGreetingChangeTime()
        }.collect {
            if(!canScheduleExactAlarm()) {
                showBatteryOptimisationNotification()
                return@collect
            }else{
                notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
            }
            val intent = GreetingTargetAlarmReceiver.createIntent(context)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                it.toEpochMilli(),
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.GREETING_ALARM.ordinal,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    private fun setupAlarmComplicationAlarms() = scope.launch {
        scheduleAlarmComplicationAlarmBus.mapNotNull {
            alarmManager.getNextAlarmChangedTime()
        }.collect {
            if(!canScheduleExactAlarm()) {
                showBatteryOptimisationNotification()
                return@collect
            }else{
                notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
            }
            val intent = AlarmComplicationAlarmReceiver.createIntent(context)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                it.toEpochMilli(),
                PendingIntent.getBroadcast(
                    context,
                    NotificationId.ALARM_COMPLICATION_ALARM.ordinal,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    override fun enqueueNextTimeDateRequirementReceiver() {
        scope.launch {
            scheduleTimeDateRequirementAlarmBus.emit(System.currentTimeMillis())
        }
    }

    override fun enqueueNextCalendarTargetReceiver() {
        scope.launch {
            scheduleCalendarTargetAlarmBus.emit(System.currentTimeMillis())
        }
    }

    override fun enqueueNextGreetingTargetReceiver() {
        scope.launch {
            scheduleGreetingTargetAlarmBus.emit(System.currentTimeMillis())
        }
    }

    override fun enqueueNextAlarmChangedReceiver() {
        scope.launch {
            scheduleAlarmComplicationAlarmBus.emit(System.currentTimeMillis())
        }
    }

    override fun enqueueNextCalendarTargetTOTMReceiver() {
        scope.launch {
            scheduleCalendarTargetTOTMAlarmBus.emit(Unit)
        }
    }

    override fun enqueueDailyUpdateReceiver() {
        if(!canScheduleExactAlarm()) {
            showBatteryOptimisationNotification()
            return
        }else{
            notificationRepository.cancelNotification(NotificationId.BATTERY_OPTIMISATION)
        }
        val intent = DailyUpdateAlarmReceiver.createIntent(context)
        val time = LocalDate.now().atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault())
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            time.toInstant().toEpochMilli(),
            PendingIntent.getBroadcast(
                context,
                NotificationId.DAILY_UPDATE_ALARM.ordinal,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

    override fun onRequirementChanged() {
        enqueueNextTimeDateRequirementReceiver()
        enqueueNextCalendarTargetReceiver()
        enqueueNextGreetingTargetReceiver()
        enqueueDailyUpdateReceiver()
    }

    /**
     *  Returns the next triggering [TimeDateRequirementData]s (may be more than one), which should
     *  have its alarm scheduled. This does not automatically update with timezone or time changes,
     *  so should be re-run when that happens.
     */
    @VisibleForTesting
    fun getNextTimeDateRequirements(): Flow<Pair<Long, List<TimeDateRequirementData>>?> {
        return timeDateRequirements.filterNotNull().mapLatest { requirements ->
            val timeList = ArrayList<Pair<Long, TimeDateRequirementData>>()
            requirements.forEach {
                timeList.add(Pair(it.getNextStartTriggerTime(), it))
                timeList.add(Pair(it.getNextEndTriggerTime(), it))
            }
            val timeMap = timeList.groupBy {
                it.first
            }.mapValues {
                it.value.map { item -> item.second }
            }
            timeMap.entries.minByOrNull { it.key }?.toPair()
        }
    }

    @VisibleForTesting
    fun TimeDateRequirementData.getNextStartTriggerTime(): Long {
        return startTime.getNextTriggerTime()
    }

    @VisibleForTesting
    fun TimeDateRequirementData.getNextEndTriggerTime(): Long {
        return endTime.getNextTriggerTime()
    }

    /**
     *  Gets the next trigger time for this LocalTime, if it's before this time today this will
     *  return a date today, otherwise it will be tomorrow.
     */
    private fun LocalTime.getNextTriggerTime(): Long {
        val now = LocalDateTime.now()
        val timeAtToday = atDate(LocalDate.now())
        return if(timeAtToday.isBefore(now)){
            atDate(LocalDate.now().plusDays(1))
        }else{
            timeAtToday
        }.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    override fun canScheduleExactAlarm(): Boolean {
        //Left open for if Google change their minds again and require it separate from battery
        return powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
    }

    @VisibleForTesting
    fun showBatteryOptimisationNotification() = with(context) {
        notificationRepository.showNotification(
            NotificationId.BATTERY_OPTIMISATION,
            NotificationChannel.ERROR
        ) {
            val notificationIntent = getIgnoreBatteryOptimisationsIntent()
            it.setContentTitle(getString(R.string.notification_battery_optimisation_title))
            it.setContentText(getString(R.string.notification_battery_optimisation_content))
            it.setSmallIcon(R.drawable.ic_warning)
            it.setAutoCancel(false)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.BATTERY_OPTIMISATION.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_battery_optimisation_title))
        }
    }

    init {
        setupTimeDateRequirementAlarms()
        setupBatteryOptimisationChange()
        setupCalendarTargetAlarms()
        setupCalendarTargetTOTMAlarms()
        setupGreetingTargetAlarms()
        setupAlarmComplicationAlarms()
        enqueueDailyUpdateReceiver()
    }

}