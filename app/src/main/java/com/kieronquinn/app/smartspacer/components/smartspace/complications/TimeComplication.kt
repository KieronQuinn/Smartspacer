package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.app.PendingIntent
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.broadcasts.TimeChangedBroadcast
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import java.util.Calendar
import android.graphics.drawable.Icon as AndroidIcon

class TimeComplication: SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOf(
            ComplicationTemplate.Basic(
                "time_at_${System.currentTimeMillis()}",
                Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_requirement_time_date)),
                Text(getTime()),
                getClickAction()
            ).create()
        )
    }

    private fun getClickAction(): TapAction {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            provideContext(),
            NotificationId.CLOCK_COMPLICATION.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return TapAction(pendingIntent = pendingIntent)
    }

    private fun getTime(): String {
        val format = DateFormat.getTimeFormat(provideContext())
        return format.format(Calendar.getInstance().time)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val description = if(smartspacerId != null) {
            resources.getText(R.string.complication_clock_description_short)
        }else{
            resources.getText(R.string.complication_clock_description)
        }
        return Config(
            resources.getString(R.string.complication_clock_label),
            description,
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_requirement_time_date),
            broadcastProvider = TimeChangedBroadcast.AUTHORITY,
            allowAddingMoreThanOnce = true
        )
    }

}