package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.utils.extensions.toSmartspaceSessionId
import com.kieronquinn.app.smartspacer.utils.extensions.toSystemSmartspaceTarget
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.inject
import android.app.smartspace.SmartspaceSessionId as SystemSmartspaceSessionId
import android.app.smartspace.SmartspaceTarget as SystemSmartspaceTarget

/**
 *  Like [SystemSmartspacerSession], but with no filtering, no settings and follows the system
 *  config.
 */
class GlanceableHubSmartspacerSession(
    context: Context,
    config: SmartspaceConfig,
    override val sessionId: SystemSmartspaceSessionId,
    private val collectInto: suspend (SystemSmartspaceSessionId, List<SystemSmartspaceTarget>) -> Unit
): BaseSmartspacerSession<SystemSmartspaceTarget, SystemSmartspaceSessionId>(
    context, config, sessionId
) {

    private val systemSmartspaceRepository by inject<SystemSmartspaceRepository>()

    //Handled by system
    override val enablePeriodicUpdates = false
    override val targetCount = flowOf(config.smartspaceTargetCount)

    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SystemSmartspaceTarget>> {
        return combine(pages, uiSurface) { p, s ->
            p.map { it.page.toSystemSmartspaceTarget(s) }
        }.flowOn(Dispatchers.IO)
    }

    override fun filterTargets(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return targets
    }

    override suspend fun collectInto(
        id: SystemSmartspaceSessionId,
        targets: List<SystemSmartspaceTarget>
    ) {
        //Don't use the regular targets as we're using our own
    }

    override fun toSmartspacerSessionId(id: SystemSmartspaceSessionId): SmartspaceSessionId {
        return id.toSmartspaceSessionId()
    }

    private fun setupMediaCollection() = whenCreated {
        systemSmartspaceRepository.mediaTargets.collect {
            collectInto.invoke(
                sessionId,
                it.map { target -> target.toSystemSmartspaceTarget(UiSurface.GLANCEABLE_HUB) }
            )
        }
    }

    init {
        onCreate()
        setupMediaCollection()
    }

}