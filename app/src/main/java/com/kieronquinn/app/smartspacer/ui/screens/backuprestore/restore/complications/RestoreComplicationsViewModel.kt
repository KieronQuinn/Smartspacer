package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.complications

import android.content.Context
import android.graphics.drawable.Icon
import android.text.SpannableStringBuilder
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModelImpl
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
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction

abstract class RestoreComplicationsViewModel(
    context: Context,
    targetsRepository: TargetsRepository,
    databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseAddComplicationsViewModelImpl(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope
) {

    abstract val state: StateFlow<State>

    abstract fun setupWithConfig(config: RestoreConfig)
    abstract fun onNextClicked()
    abstract fun onAdded(complication: Item.Complication)

}

class RestoreComplicationsViewModelImpl(
    private val navigation: ContainerNavigation,
    private val databaseRepository: DatabaseRepository,
    context: Context,
    targetsRepository: TargetsRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): RestoreComplicationsViewModel(
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
    private val addedComplicationIds = MutableStateFlow(emptySet<String>())
    private val complicationIdLock = Mutex()
    private val packageManager = context.packageManager

    private val nextIndex = databaseRepository.getActions().map { items ->
        (items.maxOfOrNull { it.index } ?: 0) + 1
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val resources = context.resources

    private val complicationBackups = config.filterNotNull().mapLatest {
        val currentComplications = targetsRepository.getAvailableComplications().first()
        it.backup.complicationBackups.mapNotNull { complication ->
            complication.toRestoreComplication(context, currentComplications)
        }
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        complicationBackups,
        addedComplicationIds
    ) { backupComplications, added ->
        backupComplications.filterNot { complication ->
            added.contains(complication.id)
        }.let {
            State.Loaded(it)
        }
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithConfig(config: RestoreConfig) {
        vmScope.launch {
            this@RestoreComplicationsViewModelImpl.config.emit(config)
        }
    }

    private suspend fun appendAddedActionId(id: String) = complicationIdLock.withLock {
        val ids = addedComplicationIds.value
        addedComplicationIds.emit(ids.plus(id))
    }

    override fun onAdded(complication: Item.Complication) {
        vmScope.launch(dispatcher) {
            val nextIndex = nextIndex.firstNotNull()
            val existingComplication = databaseRepository.getActionById(complication.id).first()
            val config = complication.backup?.config ?: Action.Config()
            val databaseAction = DatabaseAction(
                id = complication.id,
                authority = complication.authority,
                index = nextIndex,
                packageName = complication.packageName,
                anyRequirements = existingComplication?.anyRequirements ?: emptySet(),
                allRequirements = existingComplication?.allRequirements ?: emptySet(),
                showOnHomeScreen = config.showOnHomeScreen,
                showOnLockScreen = config.showOnLockScreen,
                showOnExpanded = config.showOnExpanded,
                showOnMusic = config.showOverMusic,
                expandedShowWhenLocked = config.expandedShowWhenLocked
            )
            if(complication.notificationAuthority != null){
                val notificationListener = NotificationListener(
                    complication.id, complication.packageName, complication.notificationAuthority
                )
                databaseRepository.addNotificationListener(notificationListener)
            }
            if(complication.broadcastAuthority != null){
                val broadcastListener = BroadcastListener(
                    complication.id, complication.packageName, complication.broadcastAuthority
                )
                databaseRepository.addBroadcastListener(broadcastListener)
            }
            databaseRepository.addAction(databaseAction)
            appendAddedActionId(databaseAction.id)
        }
    }

    private suspend fun Action.ComplicationBackup.toRestoreComplication(
        context: Context,
        currentComplications: List<Action>
    ): Item.Complication? {
        val provider = packageManager.resolveContentProvider(authority)
        if(provider == null){
            val title = resources.getString(R.string.plugin_action_default_label)
            val icon = Icon.createWithResource(context, R.drawable.ic_target_plugin)
            val reason = resources.getString(
                R.string.restore_complication_reason_not_installed,
                backup.name ?: authority
            )
            return Item.Complication(
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
        val collision = currentComplications.firstOrNull { it.id == id }
        //If there's a complication with the same ID but a different authority, prevent adding
        if(collision != null && collision.authority != authority) {
            return null
        }
        @Suppress("CloseAction")
        val action = Action(context, authority, null, provider.packageName, config)
        val config = action.getPluginConfig().firstNotNull()
        action.close()
        //If the complication is already added with a different ID and it can't be added twice, reject
        val alreadyAdded = !config.allowAddingMoreThanOnce
                && currentComplications.any { it.authority == authority && it.id != id }
        val compatibilityState = if(alreadyAdded){
            CompatibilityState.Incompatible(
                resources.getString(R.string.restore_complication_duplicate)
            )
        }else{
            config.compatibilityState
        }
        val label = backup.name ?: config.description
        val description = if(collision != null){
            label.withCollisionWarning()
        }else label
        return Item.Complication(
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
        append(resources.getText(R.string.restore_complication_collision))
        appendLine()
        append(this@withCollisionWarning)
    }

    override fun showWidgetPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToWidgetPermissionDialogFragment2(grant))
        }
    }

    override fun showNotificationsPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToNotificationPermissionDialogFragment4(grant))
        }
    }

    override fun onNextClicked() {
        val config = config.value ?: return
        vmScope.launch {
            val directions = when {
                config.shouldRestoreRequirements -> RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreRequirementsFragment(config)
                config.shouldRestoreExpandedCustomWidgets -> RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreWidgetsFragment(config)
                config.shouldRestoreSettings -> RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreSettingsFragment(config)
                else -> RestoreComplicationsFragmentDirections.actionRestoreComplicationsFragmentToRestoreCompleteFragment()
            }
            navigation.navigate(directions)
        }
    }

}