package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

/**
 *  Session with no modifiers, doesn't care about surface types, shows all current Targets and
 *  Complications. This is used to provide the ability to dump the current data to file.
 */
class DumpSmartspacerSession(
    context: Context,
    config: SmartspaceConfig,
    override val sessionId: SmartspaceSessionId,
    private val collectInto: suspend (List<SmartspaceTarget>) -> Unit
): BaseSmartspacerSession<SmartspaceTarget, SmartspaceSessionId>(
    context,
    config,
    sessionId
) {

    override val targetCount = flowOf(config.smartspaceTargetCount)
    override fun convert(
        pages: Flow<List<SmartspaceRepository.SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SmartspaceTarget>> {
        return combine(pages, uiSurface) { p, s ->
            p.map { it.page }
        }.flowOn(Dispatchers.IO)
    }

    override fun toSmartspacerSessionId(id: SmartspaceSessionId): SmartspaceSessionId {
        return id
    }

    override suspend fun collectInto(id: SmartspaceSessionId, targets: List<SmartspaceTarget>) {
        collectInto(targets)
    }

    init {
        onCreate()
    }

}