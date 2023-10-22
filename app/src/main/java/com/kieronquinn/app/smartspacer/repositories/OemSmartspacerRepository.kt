package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants.ENABLE_UPDATE_ACTION
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants.ENABLE_UPDATE_ACTION_ALT
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants.GSA_PACKAGE
import com.google.android.libraries.assistant.oemsmartspace.shared.OEMSmartspaceSharedConstants.UPDATE_ACTION
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.service.SmartspacerBackgroundService
import com.kieronquinn.app.smartspacer.utils.extensions.getOemSmartspaceCandidates
import com.kieronquinn.app.smartspacer.utils.extensions.getUniqueId
import com.kieronquinn.app.smartspacer.utils.extensions.packageManifestContains
import com.kieronquinn.app.smartspacer.utils.extensions.queryBroadcasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

interface OemSmartspacerRepository {

    val token: String

    fun getCompatibleApps(): Flow<List<ApplicationInfo>>
    fun getSmartspaceTarget(targetId: String): SmartspaceTarget?
    fun getSmartspaceAction(actionId: String): SmartspaceAction?

}

class OemSmartspacerRepositoryImpl(
    private val smartspaceRepository: SmartspaceRepository,
    private val grantRepository: GrantRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    private val context: Context,
    packageRepository: PackageRepository,
    private val scope: CoroutineScope = MainScope()
): OemSmartspacerRepository {

    private val packageManager = context.packageManager
    private var cachedAppsWithAction: List<ApplicationInfo>? = null
    private var cachedAppsWithProtectedBroadcast: List<ApplicationInfo>? = null

    private val packageChanged = packageRepository.onPackageChanged
        .onEach {
            cachedAppsWithProtectedBroadcast = null
            cachedAppsWithAction = null
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val token = UUID.randomUUID().toString()

    private val installedApps = packageChanged.mapLatest {
        packageManager.getOemSmartspaceCandidates()
    }.flowOn(Dispatchers.IO)

    private val appsWithAction = packageChanged.mapLatest {
        cachedAppsWithAction?.let { apps -> return@mapLatest apps }
        val intent = Intent(UPDATE_ACTION)
        packageManager.queryBroadcasts(intent).map {
            it.activityInfo.applicationInfo
        }.also {
            cachedAppsWithAction = it
        }
    }.flowOn(Dispatchers.IO)

    private val appsWithProtectedBroadcast = installedApps.mapLatest {
        cachedAppsWithProtectedBroadcast?.let { apps -> return@mapLatest apps }
        it.filter { info ->
            if(info.packageName == GSA_PACKAGE) return@filter false
            info.packageManifestContains(ENABLE_UPDATE_ACTION, ENABLE_UPDATE_ACTION_ALT)
        }.also { apps ->
            cachedAppsWithProtectedBroadcast = apps
        }
    }.flowOn(Dispatchers.IO)

    override fun getCompatibleApps(): Flow<List<ApplicationInfo>> {
        return combine(appsWithAction, appsWithProtectedBroadcast) { action, broadcast ->
            (action + broadcast).distinctBy { it.packageName }
        }.flowOn(Dispatchers.IO)
    }

    override fun getSmartspaceTarget(targetId: String): SmartspaceTarget? {
        return smartspaceRepository.targets.value.firstNotNullOfOrNull {
            val parent = it.parent
            it.targets?.firstOrNull { target ->
                target.getUniqueId(parent) == targetId
            }
        }
    }

    override fun getSmartspaceAction(actionId: String): SmartspaceAction? {
        return smartspaceRepository.actions.value.firstNotNullOfOrNull {
            val parent = it.parent
            it.actions?.firstOrNull { action ->
                action.getUniqueId(parent) == actionId
            }
        }
    }

    private fun setupServiceStart() {
        combine(
            grantRepository.grants.filterNotNull().filter { it.any { grant -> grant.oemSmartspace }},
            settingsRepository.oemSmartspaceEnabled.asFlow()
        ) { grants, enabled ->
            if(grants.isNotEmpty() && enabled) {
                SmartspacerBackgroundService.startServiceIfNeeded(context)
            }
        }.launchIn(scope)
    }

    init {
        setupServiceStart()
    }

}