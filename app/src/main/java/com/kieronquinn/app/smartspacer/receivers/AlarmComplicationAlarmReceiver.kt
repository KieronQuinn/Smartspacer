package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.complications.AlarmComplication
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity
import org.koin.core.component.KoinComponent

class AlarmComplicationAlarmReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AlarmComplicationAlarmReceiver::class.java).apply {
                applySecurity(context)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        SmartspacerComplicationProvider.notifyChange(context, AlarmComplication::class.java)
    }

}