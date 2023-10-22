package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.repositories.CalendarRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CalendarTargetAlarmReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, CalendarTargetAlarmReceiver::class.java).apply {
                applySecurity(context)
            }
        }
    }

    private val alarmRepository by inject<AlarmRepository>()
    private val calendarRepository by inject<CalendarRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        calendarRepository.updateActiveCalendarEvents()
        alarmRepository.enqueueNextCalendarTargetReceiver()
        SmartspacerTargetProvider.notifyChange(context, CalendarTarget::class.java)
    }

}