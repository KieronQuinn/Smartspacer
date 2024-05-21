package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import androidx.core.os.BuildCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Feature
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.Template
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.utils.extensions.equalsCompat
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import com.kieronquinn.app.smartspacer.utils.extensions.toSmartspaceSessionId
import com.kieronquinn.app.smartspacer.utils.extensions.toSystemSmartspaceTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject
import android.app.smartspace.SmartspaceSessionId as SystemSmartspaceSessionId
import android.app.smartspace.SmartspaceTarget as SystemSmartspaceTarget

class SystemSmartspacerSession(
    context: Context,
    config: SmartspaceConfig,
    override val sessionId: SystemSmartspaceSessionId,
    private val collectInto: suspend (SystemSmartspaceSessionId, List<SystemSmartspaceTarget>) -> Unit
): BaseSmartspacerSession<SystemSmartspaceTarget, SystemSmartspaceSessionId>(
    context, config, sessionId
) {

    private val compatibilityRepository by inject<CompatibilityRepository>()

    private val settingsTargetCount = settings.nativeTargetCountLimit.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, settings.nativeTargetCountLimit.getSync())

    private val settingsHideIncompatible = settings.nativeHideIncompatible.asFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, settings.nativeHideIncompatible.getSync())

    private val compatibility = compatibilityRepository.compatibilityReports
        .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

    private val configTargetCount = config.smartspaceTargetCount
    private val configPackageName = config.packageName
    private val configSurface = config.uiSurface

    //Handled by system
    override val enablePeriodicUpdates = false

    //AoD audio is not supported on Android 15 anymore
    @OptIn(BuildCompat.PrereleaseSdkCheck::class)
    override val supportsAodAudio = !BuildCompat.isAtLeastV()

    override val targetCount = settingsTargetCount.map {
        when(it){
            TargetCountLimit.AUTOMATIC -> configTargetCount
            else -> it.count
        }
    }

    override fun convert(
        pages: Flow<List<SmartspacePageHolder>>,
        uiSurface: Flow<UiSurface>
    ): Flow<List<SystemSmartspaceTarget>> {
        return combine(pages, uiSurface) { p, s ->
            p.map { it.page.toSystemSmartspaceTarget(s) }
        }.flowOn(Dispatchers.IO)
    }

    override fun filterTargets(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return combine(
            targets,
            settingsHideIncompatible
        ) { t, h ->
            if(h) {
                t.filterCompatible()
            } else t
        }
    }

    override fun Flow<List<SystemSmartspaceTarget>>.filterDistinct(): Flow<List<SystemSmartspaceTarget>> {
        return distinctUntilChanged { old, new ->
            //Runs our own equality check, which is more robust than the system one
            old.equalsCompat(new)
        }
    }

    private suspend fun List<TargetHolder>.filterCompatible(): List<TargetHolder> {
        return map { holder ->
            TargetHolder(holder.parent, holder.targets?.filter { target ->
                target.isCompatible()
            })
        }
    }

    private suspend fun SmartspaceTarget.isCompatible(): Boolean {
        val compatibilityReport = compatibility.firstNotNull().firstOrNull {
            it.packageName == configPackageName
        }?.compatibility ?: return true //Assume compatible if we don't have a report
        return templateData?.let { templateData ->
            compatibilityReport.mapNotNull {
                val item = it.item as? Template ?: return@mapNotNull null
                Pair(item, it.compatible)
            }.firstOrNull {
                it.first.smartspacerTemplate == templateData::class.java
            }?.second ?: true //Assume compatible if not found
        } ?: run {
            compatibilityReport.mapNotNull {
                val item = it.item as? Feature ?: return@mapNotNull null
                Pair(item, it.compatible)
            }.firstOrNull {
                it.first.feature.contains(featureType)
            }?.second ?: true //Assume compatible if not found
        }
    }

    override suspend fun collectInto(
        id: SystemSmartspaceSessionId,
        targets: List<SystemSmartspaceTarget>
    ) {
        collectInto.invoke(id, targets)
    }

    override fun toSmartspacerSessionId(id: SystemSmartspaceSessionId): SmartspaceSessionId {
        return id.toSmartspaceSessionId()
    }

    override fun doesHaveSplitSmartspace(): Boolean {
        return isAtLeastU()
    }

    /**
     *  Returns if the current surface supports splitting and splitting is enabled
     */
    private fun isInSplitMode(): Boolean {
        return configSurface == UiSurface.LOCKSCREEN && doesHaveSplitSmartspace()
    }

    /**
     *  We need to take into account the split Target, which does not induce the crash, to allow
     *  n + 1 targets
     */
    override fun List<SystemSmartspaceTarget>.trimTo(count: Int): List<SystemSmartspaceTarget> {
        //If already ignoring the value or not splitting, this makes no difference
        if(count == Integer.MAX_VALUE || !isInSplitMode()) return take(count)
        val splitTargetCount = count { it.featureType == SmartspaceTarget.FEATURE_WEATHER }
        return take(count + splitTargetCount)
    }

    /**
     *  We need to take into account the split Target, which does not induce the crash, to allow
     *  n + 1 pages
     */
    override fun List<SmartspacePageHolder>.trimPagesTo(count: Int): List<SmartspacePageHolder> {
        //If already ignoring the value or not splitting, this makes no difference
        if(count == Integer.MAX_VALUE || !isInSplitMode()) return take(count)
        val splitCount = count { it.page.featureType == SmartspaceTarget.FEATURE_WEATHER }
        return take(count + splitCount)
    }

    init {
        onCreate()
    }

}