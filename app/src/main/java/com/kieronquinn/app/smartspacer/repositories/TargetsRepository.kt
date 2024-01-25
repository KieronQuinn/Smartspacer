package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DefaultComplication
import com.kieronquinn.app.smartspacer.components.smartspace.complications.GoogleWeatherComplication
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AmmNowPlayingTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AsNowPlayingTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AtAGlanceTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_COMPLICATION
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Companion.ACTION_TARGET
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.queryContentProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID
import com.kieronquinn.app.smartspacer.model.database.Action as RawDatabaseAction
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Action as Complication

interface TargetsRepository {

    /**
     *  Whether any Smartspace is visible
     */
    val smartspaceVisible: StateFlow<Boolean>

    /**
     *  Returns a state list of the available regular targets. These should be merged with the
     *  small actions, skipping any items that already have actions set.
     */
    fun getAvailableTargets(): Flow<List<Target>>

    /**
     *  Returns a state list of the available complications, which should be merged with the targets
     */
    fun getAvailableComplications(): Flow<List<Complication>>

    /**
     *  Set whether any smartspace is currently visible. This has a small debounce on it to allow
     *  switching between lock/home without pausing and resuming.
     */
    fun setSmartspaceVisibility(visible: Boolean)

    /**
     *  Returns all available targets
     */
    fun getAllTargets(): List<Target>

    /**
     *  Returns all available complications
     */
    fun getAllComplications(): List<Complication>

    /**
     *  Notifies a given target with [id] and [authority] of a change after a delay of 1s
     */
    fun notifyTargetChangeAfterDelay(id: String, authority: String)

    /**
     *  Notifies a given complication with [id] and [authority] of a change after a delay of 1s
     */
    fun notifyComplicationChangeAfterDelay(id: String, authority: String)

    /**
     *  Triggers a reload for all targets and complications, including re-checking compatibility
     */
    fun forceReloadAll()

    /**
     *  Returns a list of recommended targets, used during setup to provide the user with some
     *  friendly options to get them started. Can be empty if setup is re-run.
     */
    suspend fun getRecommendedTargets(): List<Target>

    /**
     *  Returns a list of recommended complications, used during setup to provide the user with some
     *  friendly options to get them started. Can be empty if setup is re-run.
     */
    suspend fun getRecommendedComplications(): List<Complication>

    /**
     *  Attempts to perform a restore on a Target with a given [authority], [smartspacerId] using
     *  a [backup]
     */
    suspend fun performTargetRestore(
        authority: String, smartspacerId: String, backup: Backup
    ): Boolean

    /**
     *  Attempts to perform a restore on a Complication with a given [authority], [smartspacerId]
     *  using a [backup]
     */
    suspend fun performComplicationRestore(
        authority: String, smartspacerId: String, backup: Backup
    ): Boolean

}

class TargetsRepositoryImpl(
    private val context: Context,
    private val databaseRepository: DatabaseRepository,
    private val scope: CoroutineScope = MainScope()
): TargetsRepository {

    companion object {
        @VisibleForTesting
        val RECOMMENDED_TARGETS = arrayOf(
            DefaultTarget.AUTHORITY,
            AtAGlanceTarget.AUTHORITY,
            AsNowPlayingTarget.AUTHORITY,
            AmmNowPlayingTarget.AUTHORITY
        )

        @VisibleForTesting
        val RECOMMENDED_COMPLICATIONS = arrayOf(
            DefaultComplication.AUTHORITY,
            GoogleWeatherComplication.AUTHORITY
        )
    }

    @VisibleForTesting
    val forceReload = MutableStateFlow(System.currentTimeMillis())

    override val smartspaceVisible = MutableStateFlow(false)

    private val targets = combine(databaseRepository.getTargets(), forceReload) { targets, _ ->
        loadTargets(context, targets)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val actions = combine(databaseRepository.getActions(), forceReload) { actions, _ ->
        loadActions(context, actions)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun getAvailableTargets(): Flow<List<Target>> {
        return targets
    }

    override fun getAvailableComplications(): Flow<List<Complication>> {
        return actions
    }

    override fun setSmartspaceVisibility(visible: Boolean) {
        scope.launch {
            smartspaceVisible.emit(visible)
        }
    }

    override fun getAllTargets(): List<Target> {
        val providers = context.packageManager.queryContentProviders(Intent(ACTION_TARGET))
        return providers.map {
            @Suppress("CloseTarget")
            Target(
                context,
                it.providerInfo.authority,
                null, //ID isn't yet set
                it.providerInfo.packageName
            )
        }
    }

    override fun getAllComplications(): List<Complication> {
        val providers = context.packageManager.queryContentProviders(Intent(ACTION_COMPLICATION))
        return providers.map {
            Complication(
                context,
                it.providerInfo.authority,
                null, //ID isn't set yet
                it.providerInfo.packageName
            )
        }
    }

    override fun notifyTargetChangeAfterDelay(id: String, authority: String) {
        scope.launch {
            delay(1000L)
            SmartspacerTargetProvider.notifyChange(context, authority, id)
        }
    }

    override fun notifyComplicationChangeAfterDelay(id: String, authority: String) {
        scope.launch {
            delay(1000L)
            SmartspacerComplicationProvider.notifyChange(context, authority, id)
        }
    }

    override suspend fun getRecommendedTargets(): List<Target> = withContext(Dispatchers.IO) {
        val current = databaseRepository.getTargets().first()
        RECOMMENDED_TARGETS.map {
            @Suppress("CloseTarget")
            Target(context, it, UUID.randomUUID().toString())
        }.filterNot {
            current.any { target -> target.authority == it.authority }
        }.filter {
            it.getPluginConfig().firstNotNull().compatibilityState == CompatibilityState.Compatible
        }.removeAtAGlanceIfDefaultIsAvailable()
    }

    private fun List<Target>.removeAtAGlanceIfDefaultIsAvailable(): List<Target> {
        return filterNot {
            it.authority == AtAGlanceTarget.AUTHORITY && any { target ->
                target.authority == DefaultTarget.AUTHORITY
            }
        }
    }

    override suspend fun getRecommendedComplications(): List<Complication> = withContext(Dispatchers.IO) {
        val current = databaseRepository.getActions().first()
        RECOMMENDED_COMPLICATIONS.map {
            Complication(context, it, UUID.randomUUID().toString())
        }.filterNot {
            current.any { complication -> complication.authority == it.authority }
        }.filter {
            it.getPluginConfig().firstNotNull().compatibilityState == CompatibilityState.Compatible
        }.removeWeatherIfDefaultIsAvailable()
    }

    private fun List<Action>.removeWeatherIfDefaultIsAvailable(): List<Action> {
        return filterNot {
            it.authority == GoogleWeatherComplication.AUTHORITY && any { complication ->
                complication.authority == DefaultComplication.AUTHORITY
            }
        }
    }

    override fun forceReloadAll() {
        scope.launch {
            forceReload.emit(System.currentTimeMillis())
        }
    }

    override suspend fun performTargetRestore(
        authority: String,
        smartspacerId: String,
        backup: Backup
    ): Boolean = withContext(Dispatchers.IO) {
        @Suppress("CloseTarget")
        val target = Target(context, authority, smartspacerId)
        target.restoreBackup(backup).also {
            target.close()
        }
    }

    override suspend fun performComplicationRestore(
        authority: String,
        smartspacerId: String,
        backup: Backup
    ): Boolean = withContext(Dispatchers.IO) {
        val complication = Complication(context, authority, smartspacerId)
        complication.restoreBackup(backup)
    }

    private suspend fun loadTargets(context: Context, targets: List<DatabaseTarget>): List<Target> {
        return withContext(Dispatchers.IO) {
            targets.mapNotNull {
                val config = Target.Config(
                    showOnHomeScreen = it.showOnHomeScreen,
                    showOnLockScreen = it.showOnLockScreen,
                    showOnExpanded = it.showOnExpanded,
                    showOverMusic = it.showOnMusic,
                    showRemoteViews = it.showRemoteViews,
                    showWidget = it.showWidget,
                    showShortcuts = it.showShortcuts,
                    showAppShortcuts = it.showAppShortcuts,
                    expandedShowWhenLocked = it.expandedShowWhenLocked,
                    disableSubComplications = it.disableSubComplications
                )
                @Suppress("CloseTarget")
                val target = Target(context, it.authority, it.id, it.packageName, config)
                if(target.getPluginConfig().firstNotNull().compatibilityState != CompatibilityState.Compatible) {
                    return@mapNotNull null
                }
                target
            }
        }
    }

    private suspend fun loadActions(context: Context, actions: List<RawDatabaseAction>): List<Complication> {
        return withContext(Dispatchers.IO) {
            actions.mapNotNull {
                val config = Complication.Config(
                    showOnHomeScreen = it.showOnHomeScreen,
                    showOnLockScreen = it.showOnLockScreen,
                    showOnExpanded = it.showOnExpanded,
                    showOverMusic = it.showOnMusic,
                    expandedShowWhenLocked = it.expandedShowWhenLocked
                )
                val complication = Complication(context, it.authority, it.id, it.packageName, config)
                if(complication.getPluginConfig().firstNotNull().compatibilityState != CompatibilityState.Compatible){
                    return@mapNotNull null
                }
                complication
            }
        }
    }

    private fun setupDatabaseRefresh() {
        scope.launch {
            var targetsCache = targets.value
            targets.collect {
                targetsCache.forEach { target -> target.close() }
                targetsCache = it
            }
        }
        scope.launch {
            var actionsCache = actions.value
            actions.collect {
                actionsCache.forEach { action -> action.close() }
                actionsCache = it
            }
        }
    }

    init {
        setupDatabaseRefresh()
    }

}