package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver: BroadcastReceiver(), KoinComponent {

    private val systemSmartspaceRepository by inject<SystemSmartspaceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        systemSmartspaceRepository.showNativeStartReminderIfNeeded()
    }

}