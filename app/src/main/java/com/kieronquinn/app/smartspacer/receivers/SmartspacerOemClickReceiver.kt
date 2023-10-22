package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspacerOemClickReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        const val WIDGET_CLICK_KEY_TARGET_ID = "click_target_id"
        const val WIDGET_CLICK_KEY_ACTION_ID = "click_action_id"
    }

    private val smartspaceRepository by inject<SmartspaceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        val targetId = intent.getStringExtra(WIDGET_CLICK_KEY_TARGET_ID) ?: return
        val actionId = intent.getStringExtra(WIDGET_CLICK_KEY_ACTION_ID)
        smartspaceRepository.notifyClickEvent(targetId, actionId)
    }

}