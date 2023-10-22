package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GreetingTargetAlarmReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, GreetingTargetAlarmReceiver::class.java).apply {
                applySecurity(context)
            }
        }
    }

    private val alarmRepository by inject<AlarmRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        SmartspacerTargetProvider.notifyChange(context, GreetingTarget::class.java)
        alarmRepository.enqueueNextGreetingTargetReceiver()
    }

}