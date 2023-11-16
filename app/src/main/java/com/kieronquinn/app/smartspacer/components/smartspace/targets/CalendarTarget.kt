package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.TextUtils
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository.CalendarEvent
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.utils.extensions.atStartOfMinute
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import org.koin.android.ext.android.inject
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import android.graphics.drawable.Icon as AndroidIcon
import android.text.format.DateFormat as AndroidDateFormat

class CalendarTarget: SmartspacerTargetProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.calendar"
        private const val ID_PREFIX = "calendar_"
        private const val SHORT_TITLE_LENGTH = 30
    }

    private val dataRepository by inject<DataRepository>()
    private val calendarRepository by inject<CalendarRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val format = AndroidDateFormat.getTimeFormat(provideContext())
        val settings = getSettings(smartspacerId) ?: TargetData(smartspacerId)
        return calendarRepository.getCalendarEvents(smartspacerId).filterNot {
            settings.isDismissed(it) //Event has been dismissed
        }.map {
            it.toTarget(format, settings)
        }
    }

    private fun CalendarEvent.toTarget(
        dateFormat: DateFormat,
        settings: TargetData
    ): SmartspaceTarget {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLong())
        }
        val onClick = TapAction(intent = intent)
        val id = if(settings.useAlternativeEventIds){
            getAlternativeId()
        }else id
        val subComplication = location?.let {
            if(!settings.showLocation || location.isBlank()) return@let null
            ComplicationTemplate.Basic(
                "$ID_PREFIX$id",
                Icon(
                    AndroidIcon.createWithResource(
                        provideContext(), R.drawable.ic_target_calendar_location
                    )
                ),
                Text(location),
                onClick = onClick
            )
        }
        val title = title.ifBlank {
            resources.getString(R.string.target_calendar_title_default)
        }.formatTitle(startTime)
        return TargetTemplate.Basic(
            "$ID_PREFIX$id",
            ComponentName(provideContext(), CalendarTarget::class.java),
            SmartspaceTarget.FEATURE_CALENDAR,
            Text(title, truncateAtType = TextUtils.TruncateAt.MIDDLE),
            Text(getSubtitle(dateFormat)),
            Icon(
                AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_calendar)
            ),
            onClick = onClick,
            subComplication = subComplication?.create()
        ).create()
    }

    private fun String.formatTitle(startTime: Instant): String {
        val now = ZonedDateTime.now().atStartOfMinute().toInstant()
        val minutes = if(!startTime.isBefore(now)){
            Duration.between(now, startTime).toMinutes()
        }else 0
        return if(minutes in 1..60){
            val shortTitle = takeEllipsised(SHORT_TITLE_LENGTH)
            provideContext().getString(
                R.string.target_calendar_title_with_time, shortTitle, minutes
            )
        }else{
            this
        }
    }

    private fun CalendarEvent.getSubtitle(dateFormat: DateFormat): String {
        val now = LocalDateTime.now()
        val start = dateFormat.format(Date.from(startTime))
        val end = dateFormat.format(Date.from(endTime))
        val startTime = startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val endTime = endTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        if(isAllDay) {
            return resources.getString(R.string.target_calendar_subtitle_all_day)
        }
        return when {
            startTime.isSameDay(now) && endTime.isSameDay(now) -> "$start - $end"
            startTime.isYesterday(now) -> {
                resources.getString(
                    R.string.target_calendar_subtitle_with_yesterday,
                    start,
                    end
                )
            }
            endTime.isTomorrow(now) -> {
                resources.getString(
                    R.string.target_calendar_subtitle_with_tomorrow,
                    start,
                    end
                )
            }
            startTime.isSameDay(now) -> {
                resources.getString(
                    R.string.target_calendar_subtitle_with_date_for_end,
                    start,
                    this.endTime.getDate(),
                    end,
                )
            }
            endTime.isSameDay(now) -> {
                resources.getString(
                    R.string.target_calendar_subtitle_with_date_for_start,
                    this.startTime.getDate(),
                    start,
                    end
                )
            }
            else -> "$start - $end"
        }
    }

    private fun LocalDateTime.isSameDay(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear
    }

    private fun LocalDateTime.isYesterday(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear - 1
    }

    private fun LocalDateTime.isTomorrow(other: LocalDateTime): Boolean {
        return year == other.year && dayOfYear == other.dayOfYear + 1
    }

    private fun Instant.getDate(): String {
        val format = AndroidDateFormat.getDateFormat(provideContext())
        return format.format(Date.from(this))
    }

    override fun getConfig(smartspacerId: String?): Config {
        val settings = smartspacerId?.let { getSettings(it) }
        val label = if(settings != null){
            val names = getCalendarNames(settings.calendars.toList())
            if(names.isNotEmpty()){
                resources.getString(
                    R.string.target_calendar_description_filled, names.joinToString(", ")
                )
            }else{
                resources.getString(R.string.target_calendar_description)
            }
        }else{
            resources.getString(R.string.target_calendar_description)
        }
        return Config(
            resources.getString(R.string.target_calendar_label),
            label,
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_calendar),
            allowAddingMoreThanOnce = true,
            //Refreshing is handled by TOTM alarm, since updates do not happen on the minute
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), ConfigurationActivity.NavGraphMapping.TARGET_CALENDAR
            ),
            setupActivity = ConfigurationActivity.createIntent(
                provideContext(), ConfigurationActivity.NavGraphMapping.TARGET_CALENDAR
            )
        )
    }

    private fun getCalendarNames(calendarIds: List<String>): List<String> {
        val calendars = calendarRepository.getCurrentCalendars()
        return calendarIds.mapNotNull {
            calendars.firstOrNull { c -> c.id == it }?.name
        }.sortedBy { it.lowercase() }
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.CALENDAR,
            ::onDataChanged
        ) {
            val data = it ?: TargetData(smartspacerId)
            val eventId = targetId.removePrefix(ID_PREFIX)
            data.copy(dismissedEvents = data.dismissedEvents.plus(eventId))
        }
        return true
    }

    private fun onDataChanged(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = getSettings(smartspacerId) ?: return Backup()
        val names = getCalendarNames(settings.calendars.toList())
        val name = if(names.isNotEmpty()){
            resources.getString(
                R.string.target_calendar_description_filled, names.joinToString(", ")
            )
        }else{
            resources.getString(R.string.target_calendar_description)
        }
        return Backup(gson.toJson(settings), name)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val settings = gson.fromJson(backup.data ?: return false, TargetData::class.java)
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.CALENDAR,
            ::onDataChanged
        ) {
            settings
        }
        return false //Show setup for calendar permission
    }

    private fun getSettings(smartspacerId: String): TargetData? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)
    }

    private fun TargetData.isDismissed(calendarEvent: CalendarEvent): Boolean {
        return dismissedEvents.contains(calendarEvent.id) ||
                dismissedEvents.contains(calendarEvent.getAlternativeId())
    }

    data class TargetData(
        @SerializedName("id")
        val id: String,
        @SerializedName("pre_event_time")
        val preEventTime: PreEventTime = PreEventTime.FIFTEEN_MINUTES,
        @SerializedName("post_event_time")
        val postEventTime: PostEventTime? = PostEventTime.AT_END,
        @SerializedName("show_all_day")
        val showAllDay: Boolean = false,
        @SerializedName("show_location")
        val showLocation: Boolean = true,
        @SerializedName("show_unconfirmed")
        val showUnconfirmed: Boolean = false,
        @SerializedName("use_alternative_event_ids")
        val useAlternativeEventIds: Boolean = false,
        @SerializedName("calendars")
        val calendars: Set<String> = emptySet(),
        @SerializedName("dismissed_events")
        val dismissedEvents: Set<String> = emptySet()
    ) {
        enum class PreEventTime(val length: Duration, @StringRes val label: Int) {
            ONE_MINUTE(
                Duration.ofMinutes(1), R.string.target_calendar_pre_event_time_1_minute
            ),
            FIVE_MINUTES(
                Duration.ofMinutes(5), R.string.target_calendar_pre_event_time_5_minutes
            ),
            TEN_MINUTES(
                Duration.ofMinutes(10), R.string.target_calendar_pre_event_time_10_minutes
            ),
            FIFTEEN_MINUTES(
                Duration.ofMinutes(15), R.string.target_calendar_pre_event_time_15_minutes
            ),
            THIRTY_MINUTES(
                Duration.ofMinutes(30), R.string.target_calendar_pre_event_time_30_minutes
            ),
            ONE_HOUR(
                Duration.ofHours(1), R.string.target_calendar_pre_event_time_1_hour
            ),
            TWO_HOURS(
                Duration.ofHours(2), R.string.target_calendar_pre_event_time_2_hours
            )
        }
        enum class PostEventTime(val length: Duration?, @StringRes val label: Int) {
            AT_END(
                null, R.string.target_calendar_post_event_time_at_end
            ),
            ONE_MINUTE(
                Duration.ofMinutes(1), R.string.target_calendar_pre_event_time_1_minute
            ),
            FIVE_MINUTES(
                Duration.ofMinutes(5), R.string.target_calendar_pre_event_time_5_minutes
            ),
            TEN_MINUTES(
                Duration.ofMinutes(10), R.string.target_calendar_pre_event_time_10_minutes
            ),
            FIFTEEN_MINUTES(
                Duration.ofMinutes(15), R.string.target_calendar_pre_event_time_15_minutes
            ),
            THIRTY_MINUTES(
                Duration.ofMinutes(30), R.string.target_calendar_pre_event_time_30_minutes
            ),
            ONE_HOUR(
                Duration.ofHours(1), R.string.target_calendar_pre_event_time_1_hour
            ),
            TWO_HOURS(
                Duration.ofHours(2), R.string.target_calendar_pre_event_time_2_hours
            )
        }
    }

}