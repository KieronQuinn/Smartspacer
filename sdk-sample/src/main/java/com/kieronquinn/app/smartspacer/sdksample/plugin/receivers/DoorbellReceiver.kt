package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Doorbell

class DoorbellReceiver: SecureBroadcastReceiver() {

    private val doorbell = Doorbell.getInstance()

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        doorbell.incrementState(context)
    }

}