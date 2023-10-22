package com.kieronquinn.app.smartspacer.receivers

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DigitalWellbeingTarget
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspaceTargetUpdateReceiver: SmartspacerTargetUpdateReceiver(), KoinComponent {

    private val wellbeingRepository by inject<DigitalWellbeingRepository>()

    override fun onRequestSmartspaceTargetUpdate(
        context: Context,
        requestTargets: List<RequestTarget>
    ) {
        requestTargets.forEach {
            when(it.authority){
                DigitalWellbeingTarget.AUTHORITY -> {
                    wellbeingRepository.refreshWidgetIfNeeded(it.smartspacerId)
                }
            }
        }
    }
}