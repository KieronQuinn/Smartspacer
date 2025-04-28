package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.ui.activities.FlashlightToggleActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FlashlightReceiver: BroadcastReceiver(), KoinComponent {

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        shizukuServiceRepository.runWithServiceIfAvailable {
            it.toggleTorch()
        }.unwrap() ?: run {
            context.startActivity(Intent(context, FlashlightToggleActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }

}