package com.kieronquinn.app.smartspacer.receivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.complications.AlarmComplication
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmChangedReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        private val ALARM_BROADCASTS = arrayOf(
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }

    private val alarmRepository by inject<AlarmRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        if(!ALARM_BROADCASTS.contains(intent.action)) return
        alarmRepository.enqueueNextAlarmChangedReceiver()
        //Refresh the complication in case the alarm trigger time has already passed
        SmartspacerComplicationProvider.notifyChange(context, AlarmComplication::class.java)
    }

}