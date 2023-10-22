package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DummyReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //Do nothing, this is purely to absorb events
    }

}