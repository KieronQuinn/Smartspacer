package com.kieronquinn.app.smartspacer.utils.extensions

import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import android.app.smartspace.SmartspaceTargetEvent as SystemSmartspaceTargetEvent

fun SystemSmartspaceTargetEvent.toSystemSmartspaceTargetEvent(): SmartspaceTargetEvent {
    return SmartspaceTargetEvent(
        smartspaceTarget = smartspaceTarget?.toSmartspaceTarget(),
        smartspaceActionId = smartspaceActionId,
        eventType = eventType
    )
}