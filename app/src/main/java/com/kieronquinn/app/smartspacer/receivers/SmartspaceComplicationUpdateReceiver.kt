package com.kieronquinn.app.smartspacer.receivers

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DigitalWellbeingComplication
import com.kieronquinn.app.smartspacer.repositories.DigitalWellbeingRepository
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspaceComplicationUpdateReceiver: SmartspacerComplicationUpdateReceiver(), KoinComponent {

    private val wellbeingRepository by inject<DigitalWellbeingRepository>()

    override fun onRequestSmartspaceComplicationUpdate(
        context: Context,
        requestComplications: List<RequestComplication>
    ) {
        requestComplications.filter { DigitalWellbeingComplication.AUTHORITY == it.authority }
            .forEach { wellbeingRepository.refreshWidgetIfNeeded(it.smartspacerId) }
    }

}