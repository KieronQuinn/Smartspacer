package com.kieronquinn.app.smartspacer.components.smartspace.broadcasts

import android.content.Intent
import android.content.IntentFilter
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.complications.TimeComplication
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider

class TimeChangedBroadcast: SmartspacerBroadcastProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.broadcasts.time"

        private val ACTIONS = arrayOf(
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }

    override fun onReceive(intent: Intent) {
        SmartspacerComplicationProvider.notifyChange(provideContext(), TimeComplication::class.java)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(
            ACTIONS.map { IntentFilter(it) }
        )
    }

}