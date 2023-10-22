package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.TimeDateRequirement
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimeDateRequirementAlarmReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        private const val EXTRA_IDS = "ids"

        fun createIntent(context: Context, ids: Array<String>): Intent {
            return Intent(context, TimeDateRequirementAlarmReceiver::class.java).apply {
                applySecurity(context)
                putExtra(EXTRA_IDS, ids)
            }
        }
    }

    private val alarmRepository by inject<AlarmRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        val ids = intent.getStringArrayExtra(EXTRA_IDS) ?: return
        ids.forEach { id ->
            SmartspacerRequirementProvider.notifyChange(
                context, TimeDateRequirement::class.java, id
            )
        }
        alarmRepository.enqueueNextTimeDateRequirementReceiver()
    }

}