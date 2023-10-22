package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Stopwatch

class StopwatchReceiver: SecureBroadcastReceiver() {

    private val stopwatch = Stopwatch.getInstance()

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        stopwatch.toggleStopwatch(context)
    }

}