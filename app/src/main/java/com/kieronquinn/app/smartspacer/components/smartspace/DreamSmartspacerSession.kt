package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import kotlinx.coroutines.flow.StateFlow
import android.app.smartspace.SmartspaceSessionId as SystemSmartspaceSessionId
import android.app.smartspace.SmartspaceTarget as SystemSmartspaceTarget


class DreamSmartspacerSession(
    context: Context,
    config: SmartspaceConfig,
    override val sessionId: SystemSmartspaceSessionId,
    collectInto: suspend (SystemSmartspaceSessionId, List<SystemSmartspaceTarget>) -> Unit
): PassthroughSmartspacerSession(context, config, sessionId, collectInto) {

    override val surface = UiSurface.DREAM

    override fun SystemSmartspaceRepository.getSystemTargets(): StateFlow<List<SmartspaceTarget>> {
        return hubTargets
    }

}