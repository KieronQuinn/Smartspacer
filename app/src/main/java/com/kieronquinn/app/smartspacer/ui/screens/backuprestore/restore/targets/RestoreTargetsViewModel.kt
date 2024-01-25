package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.targets

import android.content.Context
import android.graphics.drawable.Icon
import android.text.SpannableStringBuilder
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.model.smartspace.Target.TargetBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.resolveContentProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

abstract class RestoreTargetsViewModel(
    context: Context,
    targetsRepository: TargetsRepository,
    databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseAddTargetsViewModelImpl(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    abstract val state: StateFlow<State>

    abstract fun setupWithConfig(config: RestoreConfig)
    abstract fun onNextClicked()
    abstract fun onAdded(target: Item.Target)

}

class RestoreTargetsViewModelImpl(
    private val navigation: ContainerNavigation,
    private val databaseRepository: DatabaseRepository,
    context: Context,
    targetsRepository: TargetsRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): RestoreTargetsViewModel(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    private val config = MutableStateFlow<RestoreConfig?>(null)
    private val addedTargetIds = MutableStateFlow(emptySet<String>())
    private val targetIdLock = Mutex()
    private val packageManager = context.packageManager
    private val resources = context.resources

    private val nextIndex = databaseRepository.getTargets().map { items ->
        (items.maxOfOrNull { it.index } ?: 0) + 1
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val targetBackups = config.filterNotNull().mapLatest {
        val currentTargets = targetsRepository.getAvailableTargets().first()
        it.backup.targetBackups.mapNotNull { target ->
            target.toRestoreTarget(context, currentTargets)
        }
    }.flowOn(dispatcher)

    override val state = combine(
        targetBackups,
        addedTargetIds
    ) { backupTargets, added ->
        backupTargets.filterNot { target ->
            added.contains(target.id)
        }.let {
            State.Loaded(it)
        }
    }.flowOn(dispatcher).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithConfig(config: RestoreConfig) {
        vmScope.launch {
            this@RestoreTargetsViewModelImpl.config.emit(config)
        }
    }

    private suspend fun appendAddedTargetId(id: String) = targetIdLock.withLock {
        val ids = addedTargetIds.value
        addedTargetIds.emit(ids.plus(id))
    }

    override fun onAdded(target: Item.Target) {
        vmScope.launch(dispatcher) {
            val nextIndex = nextIndex.firstNotNull()
            val existingTarget = databaseRepository.getTargetById(target.id).first()
            val config = target.backup?.config ?: Target.Config()
            val databaseTarget = DatabaseTarget(
                id = target.id,
                authority = target.authority,
                index = nextIndex,
                packageName = target.packageName,
                anyRequirements = existingTarget?.anyRequirements ?: emptySet(),
                allRequirements = existingTarget?.allRequirements ?: emptySet(),
                showOnHomeScreen = config.showOnHomeScreen,
                showOnLockScreen = config.showOnLockScreen,
                showOnExpanded = config.showOnExpanded,
                showOnMusic = config.showOverMusic,
                showRemoteViews = config.showRemoteViews,
                showWidget = config.showWidget,
                showShortcuts = config.showShortcuts,
                showAppShortcuts = config.showAppShortcuts,
                expandedShowWhenLocked = config.expandedShowWhenLocked,
                disableSubComplications = config.disableSubComplications
            )
            if(target.notificationAuthority != null){
                val notificationListener = NotificationListener(
                    target.id, target.packageName, target.notificationAuthority
                )
                databaseRepository.addNotificationListener(notificationListener)
            }
            if(target.broadcastAuthority != null){
                val broadcastListener = BroadcastListener(
                    target.id, target.packageName, target.broadcastAuthority
                )
                databaseRepository.addBroadcastListener(broadcastListener)
            }
            databaseRepository.addTarget(databaseTarget)
            appendAddedTargetId(databaseTarget.id)
        }
    }

    private suspend fun TargetBackup.toRestoreTarget(
        context: Context,
        currentTargets: List<Target>
    ): Item.Target? {
        val provider = packageManager.resolveContentProvider(authority)
        if(provider == null){
            val title = resources.getString(R.string.plugin_target_default_label)
            val icon = Icon.createWithResource(context, R.drawable.ic_target_plugin)
            val reason = resources.getString(
                R.string.restore_target_reason_not_installed, backup.name ?: authority
            )
            return Item.Target(
                "",
                authority,
                id,
                title,
                reason,
                icon,
                CompatibilityState.Incompatible(reason),
                null,
                null,
                null,
                null,
                backup = this
            )
        }
        val collision = currentTargets.firstOrNull { it.id == id }
        //If there's a target with the same ID but a different authority, prevent adding
        if(collision != null && collision.authority != authority) {
            return null
        }
        @Suppress("CloseTarget")
        val target = Target(context, authority, null, provider.packageName, config)
        val config = target.getPluginConfig().firstNotNull()
        target.close()
        //If the target is already added with a different ID and it can't be added twice, reject
        val alreadyAdded = !config.allowAddingMoreThanOnce
                && currentTargets.any { it.authority == authority && it.id != id }
        val compatibilityState = if(alreadyAdded){
            CompatibilityState.Incompatible(resources.getString(R.string.restore_target_duplicate))
        }else{
            config.compatibilityState
        }
        val label = backup.name ?: config.description
        val description = if(collision != null){
            label.withCollisionWarning()
        }else label
        return Item.Target(
            provider.packageName,
            authority,
            id,
            config.label,
            description,
            config.icon,
            compatibilityState,
            config.setupActivity,
            config.widgetProvider,
            config.notificationProvider,
            config.broadcastProvider,
            backup = this,
            hasCollision = collision != null
        )
    }

    private fun CharSequence.withCollisionWarning(): CharSequence = SpannableStringBuilder().apply {
        append(resources.getText(R.string.restore_target_collision))
        appendLine()
        append(this@withCollisionWarning)
    }

    override fun onNextClicked() {
        val config = config.value ?: return
        vmScope.launch {
            val directions = when {
                config.shouldRestoreComplications -> RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreComplicationsFragment(config)
                config.shouldRestoreRequirements -> RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreRequirementsFragment(config)
                config.shouldRestoreExpandedCustomWidgets -> RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreWidgetsFragment(config)
                config.shouldRestoreSettings -> RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreSettingsFragment(config)
                else -> RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToRestoreCompleteFragment()
            }
            navigation.navigate(directions)
        }
    }

    override fun showWidgetPermissionsDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToWidgetPermissionDialogFragment2(grant))
        }
    }

    override fun showNotificationPermissionsDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(RestoreTargetsFragmentDirections.actionRestoreTargetsFragmentToNotificationPermissionDialogFragment4(grant))
        }
    }

}