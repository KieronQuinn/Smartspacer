package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.content.Context
import android.text.format.DateFormat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import android.graphics.drawable.Icon as AndroidIcon

class AlarmComplication: SmartspacerComplicationProvider() {

    companion object {
        private val TWELVE_HOURS = Duration.ofHours(12)

        /**
         *  Gets the next time that the next alarm will change, or null if it has already passed.
         *  In the event it's already passed, the Alarm Complication will be shown anyway, and
         *  the next alarm broadcast will be sent by the system once it's gone off, triggering
         *  another call to this method which will then return the next alarm's change time.
         */
        fun AlarmManager.getNextAlarmChangedTime(): Instant? {
            return Instant.ofEpochMilli(nextAlarmClock?.triggerTime ?: return null)
                .minusMillis(TWELVE_HOURS.toMillis()).takeIf {
                    it.isAfter(Instant.now())
                }
        }
    }

    private val alarmManager by lazy {
        provideContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val timeFormat by lazy {
        DateFormat.getTimeFormat(provideContext())
    }

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOfNotNull(getNextAlarm()?.toTarget())
    }

    private fun AlarmClockInfo.toTarget(): SmartspaceAction {
        val date = Date.from(Instant.ofEpochMilli(triggerTime))
        return ComplicationTemplate.Basic(
            "alarm",
            Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_alarm)),
            Text(timeFormat.format(date)),
            TapAction(pendingIntent = showIntent)
        ).create()
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.complication_alarm_label),
            resources.getString(R.string.complication_alarm_description),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_alarm),
            allowAddingMoreThanOnce = true
        )
    }

    private fun getNextAlarm(): AlarmClockInfo? {
        val now = ZonedDateTime.now()
        return alarmManager.nextAlarmClock?.takeIf {
            it.showIntent != null
        }?.takeIf {
            val time = Instant.ofEpochMilli(it.triggerTime).atZone(ZoneId.systemDefault())
            Duration.between(now, time).abs() <= TWELVE_HOURS
        }
    }

}