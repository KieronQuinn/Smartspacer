package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 *  Triggered when something that will impact Alarms changes:
 *  - Device has booted
 *  - Time changed
 *  - Timezone changed
 *
 *  This triggers a re-schedule of alarms with the new time
 */
class AlarmRequirementChangeReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        private val ALLOWLIST_ACTIONS = arrayOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }

    private val alarmRepository by inject<AlarmRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        if(!ALLOWLIST_ACTIONS.contains(intent.action)) return
        alarmRepository.onRequirementChanged()
    }

}