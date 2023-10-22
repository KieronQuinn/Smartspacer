package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider

/**
 *  Example of how to use [SmartspacerBroadcastProvider]. This Provider requests broadcast events
 *  for [Intent.ACTION_POWER_CONNECTED] and [Intent.ACTION_POWER_DISCONNECTED], both of which
 *  are implicit intents and could not normally be registered without your Plugin using a background
 *  service.
 */
class ExampleBroadcastReceiver: SmartspacerBroadcastProvider() {

    override fun onReceive(intent: Intent) {
        Log.d("ExampleBR", "Received broadcast with action ${intent.action}")
    }

    override fun getConfig(smartspacerId: String): Config {
        val actions = arrayOf(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED)
        return Config(listOf(IntentFilter().addActions(*actions)))
    }

    private fun IntentFilter.addActions(vararg action: String): IntentFilter {
        action.forEach {
            addAction(it)
        }
        return this
    }

}