package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.content.Context
import android.provider.CalendarContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.Calendar
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarData
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarEvent
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarTrigger
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.calendar.RFC2245Duration
import com.kieronquinn.app.smartspacer.utils.extensions.atStartOfMinute
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.map
import com.kieronquinn.app.smartspacer.utils.extensions.queryAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.VisibleForTesting
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs

interface CalendarRepository {

    fun checkPermission()
    fun reloadEvents()
    fun hasPermission(): Boolean
    fun updateActiveCalendarEvents()
    fun getCalendars(): Flow<List<Calendar>>
    fun getCurrentCalendars(): List<Calendar>
    fun getNextCalendarTrigger(): CalendarTrigger?
    fun getCalendarEvents(smartspacerId: String): List<CalendarEvent>
    fun enqueueTOTMAlarmIfNeeded()

    data class Calendar(
        val id: String,
        val name: String,
        val account: String
    )

    data class CalendarEvent(
        val calendar: Calendar,
        val id: String,
        val title: String,
        val startTime: Instant,
        val endTime: Instant,
        val location: String?,
        val isAllDay: Boolean,
        val status: Int
    ) {
        val length: Duration = Duration.between(startTime, endTime)

        /**
         *  The alternative ID for an event is based on the title, start and end times rather than
         *  the system-provided ID. This allows for solutions which "sync" calendars by deleting &
         *  recreating events, which would normally lead to different IDs.
         */
        fun getAlternativeId(): String {
            return abs("$title$startTime$endTime".hashCode()).toString()
        }
    }

    data class CalendarData(
        val data: TargetData,
        val events: List<CalendarEvent>
    )

    data class CalendarTrigger(
        val time: Instant,
        val events: List<CalendarData>
    )

}

class CalendarRepositoryImpl(
    private val context: Context,
    dataRepository: DataRepository,
    private val scope: CoroutineScope = MainScope()
): CalendarRepository, KoinComponent {

    private val alarmRepository by inject<AlarmRepository>()

    @VisibleForTesting
    val allCalendarEvents = HashMap<String, CalendarData>()

    @VisibleForTesting
    val activeCalendarEvents = HashMap<String, CalendarData>()

    @VisibleForTesting
    val reloadEventsBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val hasPermission = MutableStateFlow(hasPermission())

    private val calendars = hasPermission.flatMapLatest {
        if(!it) return@flatMapLatest flowOf(emptyList())
        context.contentResolver.queryAsFlow(
            CalendarContract.Calendars.CONTENT_URI,
            projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
            )
        ).flowOn(Dispatchers.IO).debounce(250L).mapLatest { calendar ->
            calendar.map { row ->
                Calendar(
                    row.getString(0) ?: return@map null,
                    row.getString(1) ?: return@map null,
                    row.getString(2) ?: return@map null
                )
            }.filterNotNull()
        }
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    private val calendarListeners = dataRepository.getTargetData(
        TargetDataType.CALENDAR, TargetData::class.java
    ).flatMapLatest {
        it.map { data ->
            getCalendarData(data)
        }.let { listeners ->
            merge(*listeners.toTypedArray())
        }
    }

    private fun getCalendarData(targetData: TargetData): Flow<CalendarData> {
        val events = combine(
            calendars.filterNotNull(),
            reloadEventsBus
        ) { calendars, _ ->
            if(calendars.isEmpty()) return@combine emptyList()
            targetData.calendars.mapNotNull { id ->
                calendars.firstOrNull { c -> c.id == id }
            }.map { c ->
                c.getEventsForNextTwoDays()
            }
        }
        return events.flatMapLatest { eventList ->
            if(eventList.isEmpty()) return@flatMapLatest emptyFlow()
            combine(*eventList.toTypedArray()) {
                it.toList()
            }.map { events ->
                CalendarData(targetData, events.flatten())
            }
        }
    }

    override fun checkPermission() {
        scope.launch {
            hasPermission.emit(hasPermission())
        }
    }

    override fun reloadEvents() {
        scope.launch {
            reloadEventsBus.emit(System.currentTimeMillis())
        }
    }

    override fun updateActiveCalendarEvents() {
        allCalendarEvents.entries.forEach {
            activeCalendarEvents[it.key] = it.value.copy(
                events = it.value.filter()
            )
            SmartspacerTargetProvider.notifyChange(context, CalendarTarget::class.java, it.key)
        }
        alarmRepository.enqueueNextCalendarTargetReceiver()
        enqueueTOTMAlarmIfNeeded()
    }

    private fun CalendarData.filter(): List<CalendarEvent> {
        val preEventTime = data.preEventTime.length
        val showAllDay = data.showAllDay
        val showUnconfirmed = data.showUnconfirmed
        val now = ZonedDateTime.now().atStartOfMinute().toInstant()
        return events.filter {
            it.endTime == now || it.endTime.isAfter(now)
        }.filter {
            it.status == CalendarContract.Events.STATUS_CONFIRMED || showUnconfirmed
        }.filter {
            !it.isAllDay || showAllDay
        }.filter {
            it.startTime.isBefore(now) || Duration.between(now, it.startTime) <= preEventTime
        }
    }

    override fun getCalendars(): Flow<List<Calendar>> {
        return calendars.filterNotNull()
    }

    override fun getCurrentCalendars(): List<Calendar> {
        return runBlocking {
            calendars.firstNotNull()
        }
    }

    override fun getNextCalendarTrigger(): CalendarTrigger? {
        val now = Instant.now()
        val allEvents = ArrayList<Triple<Instant, CalendarData, CalendarEvent>>()
        allCalendarEvents.entries.forEach {
            val preEventTime = it.value.data.preEventTime.length
            it.value.events.forEach { event ->
                //The Target will be shown at the start - pre-event time, so take that into account
                val eventStartTime = event.startTime - preEventTime
                if(!event.isAllDay && eventStartTime.isAfter(now)) {
                    allEvents.add(Triple(eventStartTime, it.value, event))
                }
                //Include the whole end minute so it doesn't disappear exactly on the end time
                val eventEndTime = event.endTime.plusMillis(60_000L)
                if(eventEndTime.isAfter(now)) {
                    allEvents.add(Triple(eventEndTime, it.value, event))
                }
            }
        }
        val eventsByTime = allEvents.groupBy {
            it.first
        }.mapValues {
            it.value.map { event ->
                event.second
            }
        }
        return eventsByTime.entries.minByOrNull { it.key }?.let {
            CalendarTrigger(it.key, it.value)
        }
    }

    override fun getCalendarEvents(smartspacerId: String): List<CalendarEvent> {
        return activeCalendarEvents[smartspacerId]?.events ?: emptyList()
    }

    override fun enqueueTOTMAlarmIfNeeded() {
        if(hasActiveEventsThatHaveNotStartedYet()){
            alarmRepository.enqueueNextCalendarTargetTOTMReceiver()
        }
    }

    private fun hasActiveEventsThatHaveNotStartedYet(): Boolean {
        val now = Instant.now()
        return activeCalendarEvents.values.map {
            it.events
        }.flatten().any {
            it.startTime.isAfter(now)
        }
    }

    private fun Calendar.getEventsForNextTwoDays(): Flow<List<CalendarEvent>> {
        if(!hasPermission()) return flowOf(emptyList())
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        val startOfTwoDays = startOfToday.plusDays(2)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendEncodedPath(startOfToday.toEpochMilli().toString())
            .appendEncodedPath(startOfTwoDays.toEpochMilli().toString())
            .build()
        return context.contentResolver.queryAsFlow(
            uri,
            projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DTSTART,
                CalendarContract.Instances.DTEND,
                CalendarContract.Instances.DURATION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.STATUS,
                CalendarContract.Instances.LAST_DATE
            ),
            selection = "${CalendarContract.Events.CALENDAR_ID}=?",
            selectionArgs = arrayOf(id)
        ).flowOn(Dispatchers.IO).debounce(250L).mapLatest {
            it.map { event ->
                val startTime = Instant.ofEpochMilli(event.getLong(2))
                var endTime = event.getLongOrNull(3)?.let { end ->
                    Instant.ofEpochMilli(end)
                }
                //Duration is used for events which are recurring rather than one off, in RFC format
                val duration = event.getStringOrNull(4)?.let {
                    RFC2245Duration().apply { parse(it) }.millis
                }
                if(duration != null && endTime == null) {
                    endTime = startTime.plusMillis(duration)
                }
                //If the duration is still not set, fall back to the unreliable LAST_DATE field
                val lastDate = event.getLongOrNull(8)
                if(lastDate != null && endTime == null) {
                    endTime = Instant.ofEpochMilli(lastDate)
                }
                if(endTime == null) return@map null
                //All day flag isn't always accurate, so we need to check the times too
                val isAllDay = event.getInt(6) == 1 || isAllDay(startTime, endTime)
                CalendarEvent(
                    this,
                    event.getString(0),
                    event.getStringOrNull(1)
                        ?: context.getString(R.string.target_calendar_title_default),
                    startTime,
                    endTime,
                    event.getString(5),
                    isAllDay,
                    event.getInt(7)
                )
            }.filterNotNull().filterNot { event ->
                event.status == CalendarContract.Events.STATUS_CANCELED
            }
        }
    }

    private fun isAllDay(startTime: Instant, endTime: Instant): Boolean {
        val localStartTime = startTime.atZone(ZoneOffset.systemDefault())
        val localEndTime = endTime.atZone(ZoneOffset.systemDefault())
        val midnightToday = LocalDate.now().atStartOfDay().atZone(ZoneOffset.systemDefault())
        //If start not at or before midnight today, not all day
        if(localStartTime != midnightToday && !localStartTime.isBefore(midnightToday)) return false
        val midnightTomorrow = LocalDate.now().plusDays(1).atStartOfDay()
            .atZone(ZoneOffset.systemDefault())
        //If end not at or after midnight tomorrow, not all day
        if(localEndTime != midnightTomorrow && !localEndTime.isAfter(midnightTomorrow)) return false
        return true
    }

    private fun ZonedDateTime.toEpochMilli(): Long {
        return toInstant().toEpochMilli()
    }

    private fun setupCalendarListeners() = scope.launch {
        calendarListeners.collect {
            allCalendarEvents[it.data.id] = it
            updateActiveCalendarEvents()
        }
    }

    override fun hasPermission(): Boolean {
        return context.hasPermission(Manifest.permission.READ_CALENDAR)
    }

    init {
        setupCalendarListeners()
    }

}