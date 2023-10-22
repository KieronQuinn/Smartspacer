package com.kieronquinn.app.smartspacer.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.mockk.mockk

/**
 *  Simple mock Broadcast manager that can take receivers and trigger them for intents
 */
class TestBroadcastManager(private val context: Context = mockk(relaxed = true)) {

    private val receivers = HashMap<IntentFilter, BroadcastReceiver>()

    fun registerReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter): Intent {
        receivers[intentFilter] = receiver
        return intentFilter.toIntent()
    }

    fun sendBroadcast(intent: Intent) {
        receivers.forEach {
            val filter = it.key
            intent.action?.let { action ->
                if(!filter.matchAction(action)) return@forEach
            }
            intent.categories?.let { categories ->
                if(filter.matchCategories(categories) != null) return@forEach
            }
            it.value.onReceive(context, intent)
        }
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        receivers.filterValues {
            it == receiver
        }.forEach {
            receivers.remove(it.key)
        }
    }

    private fun IntentFilter.toIntent(): Intent {
        return Intent().apply {
            actionsIterator()?.forEach {
                addAction(it)
            }
            categoriesIterator()?.forEach {
                addCategory(it)
            }
        }
    }

}