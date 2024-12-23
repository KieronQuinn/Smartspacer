package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.broadcasts.TimeChangedBroadcast
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import java.util.Calendar
import android.graphics.drawable.Icon as AndroidIcon

class TimeTarget: SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return listOf(
            TargetTemplate.Basic(
                id = "time_at_${System.currentTimeMillis()}",
                componentName = ComponentName(provideContext(), TimeTarget::class.java),
                icon = Icon(
                    AndroidIcon.createWithResource(
                        provideContext(),
                        R.drawable.ic_requirement_time_date
                    )
                ),
                title = Text(getTime()),
                subtitle = null,
                onClick = getClickAction()
            ).create().apply {
                canTakeTwoComplications = true
                canBeDismissed = false
            }
        )
    }

    private fun getClickAction(): TapAction {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            provideContext(),
            NotificationId.CLOCK_TARGET.ordinal,
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
            resources.getText(R.string.target_clock_description_short)
        }else{
            resources.getText(R.string.target_clock_description)
        }
        return Config(
            resources.getString(R.string.target_clock_label),
            description,
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_requirement_time_date),
            broadcastProvider = TimeChangedBroadcast.AUTHORITY,
            allowAddingMoreThanOnce = true
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

}