package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Timer

class TimerReceiver: SecureBroadcastReceiver() {

    private val timer = Timer.getInstance()

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        timer.toggleTimer(context)
    }

}