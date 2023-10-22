package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.text.SpannableStringBuilder
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement.RequirementBackup.RequirementType
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.base.StateViewModel
import com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.requirements.RestoreRequirementsViewModel.State
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction
import com.kieronquinn.app.smartspacer.model.database.Requirement as DatabaseRequirement
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

abstract class RestoreRequirementsViewModel(scope: CoroutineScope?): StateViewModel<State>(scope) {

    abstract val addState: StateFlow<AddState>

    abstract fun setupWithConfig(config: RestoreConfig)
    abstract fun onNextClicked()

    abstract fun onRequirementClicked(
        requirement: Item,
        skipRestore: Boolean = false,
        hasRestored: Boolean = false
    )

    abstract fun onConfigureResult(context: Context, success: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>): State()
    }

    sealed class AddState {
        object Idle: AddState()
        data class Configure(val requirement: Item): AddState()
    }

    data class Item(
        val id: String,
        val authority: String,
        val packageName: String,
        val label: CharSequence,
        val description: CharSequence,
        val icon: Icon,
        val compatibilityState: CompatibilityState,
        val setupIntent: Intent?,
        val invert: Boolean,
        val backup: RequirementBackup,
        val requirementForTarget: DatabaseTarget? = null,
        val requirementForComplication: DatabaseAction? = null,
        val requirementForLabel: CharSequence? = null
    )

}

class RestoreRequirementsViewModelImpl(
    private val databaseRepository: DatabaseRepository,
    private val requirementsRepository: RequirementsRepository,
    private val navigation: ContainerNavigation,
    context: Context,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): RestoreRequirementsViewModel(scope) {

    private val packageManager = context.packageManager
    private val resources = context.resources
    private val addedRequirementIds = MutableStateFlow(emptySet<String>())
    private val requirementIdLock = Mutex()
    private val config = MutableStateFlow<RestoreConfig?>(null)

    private val requirementBackups = config.filterNotNull().mapLatest {
        val currentRequirements = databaseRepository.getRequirements().first()
        val currentTargets = databaseRepository.getTargets().first()
        val currentComplications = databaseRepository.getActions().first()
        it.backup.requirementBackups.mapNotNull { requirement ->
            requirement.toItem(context, currentRequirements, currentTargets, currentComplications)
        }
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        requirementBackups,
        addedRequirementIds
    ) { backupRequirements, added ->
        backupRequirements.filterNot { requirement ->
            added.contains(requirement.id)
        }.let {
            State.Loaded(it)
        }
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val addState = MutableStateFlow<AddState>(AddState.Idle)

    override fun onRequirementClicked(
        requirement: Item,
        skipRestore: Boolean,
        hasRestored: Boolean
    ) {
        vmScope.launch {
            when {
                !skipRestore -> {
                    performRestore(requirement)
                }
                requirement.setupIntent != null && !hasRestored -> {
                    addState.emit(AddState.Configure(requirement))
                }
                else -> {
                    addRequirement(requirement)
                }
            }
        }
    }

    private suspend fun performRestore(requirement: Item) {
        val hasRestored = requirementsRepository.performRequirementRestore(
            requirement.authority, requirement.id, requirement.invert, requirement.backup.backup
        )
        onRequirementClicked(requirement, true, hasRestored)
    }

    private suspend fun addRequirement(requirement: Item) = withContext(dispatcher) {
        val databaseRequirement = DatabaseRequirement(
            requirement.id,
            requirement.authority,
            requirement.packageName,
            requirement.backup.invert
        )
        databaseRepository.addRequirement(databaseRequirement)
        when {
            requirement.requirementForTarget != null -> {
                addRequirementToTarget(requirement, requirement.requirementForTarget.id)
            }
            requirement.requirementForComplication != null -> {
                addRequirementToComplication(requirement, requirement.requirementForComplication.id)
            }
        }
        appendAddedRequirementId(requirement.id)
        addState.emit(AddState.Idle)
    }

    private suspend fun addRequirementToTarget(requirement: Item, id: String) {
        val target = databaseRepository.getTargetById(id).first() ?: return
        when(requirement.backup.requirementType) {
            RequirementType.ANY -> {
                target.anyRequirements = target.anyRequirements.plus(requirement.id)
            }
            RequirementType.ALL -> {
                target.allRequirements = target.allRequirements.plus(requirement.id)
            }
        }
        databaseRepository.updateTarget(target)
    }

    private suspend fun addRequirementToComplication(requirement: Item, id: String) {
        val complication = databaseRepository.getActionById(id).first() ?: return
        when(requirement.backup.requirementType) {
            RequirementType.ANY -> {
                complication.anyRequirements = complication.anyRequirements.plus(requirement.id)
            }
            RequirementType.ALL -> {
                complication.allRequirements = complication.allRequirements.plus(requirement.id)
            }
        }
        databaseRepository.updateAction(complication)
    }

    override fun onConfigureResult(context: Context, success: Boolean) {
        vmScope.launch {
            val requirement = (addState.value as? AddState.Configure)?.requirement ?: return@launch
            if(!success){
                cleanupRequirement(context, requirement)
                addState.emit(AddState.Idle)
                return@launch
            }
            onRequirementClicked(requirement, skipRestore = true, hasRestored = true)
        }
    }

    private suspend fun cleanupRequirement(
        context: Context,
        requirement: Item
    ) = withContext(dispatcher) {
        @Suppress("CloseRequirement")
        val remoteRequirement = Requirement(
            context,
            requirement.authority,
            requirement.id,
            requirement.invert
        )
        remoteRequirement.onDeleted().also {
            remoteRequirement.close()
        }
    }

    private suspend fun RequirementBackup.toItem(
        context: Context,
        currentRequirements: List<DatabaseRequirement>,
        currentTargets: List<DatabaseTarget>,
        currentComplications: List<DatabaseAction>
    ): Item? {
        val provider = packageManager.resolveContentProvider(authority)
        if(provider == null){
            val title = resources.getString(R.string.plugin_requirement_default_label)
            val icon = Icon.createWithResource(context, R.drawable.ic_target_plugin)
            val reason = resources.getString(
                R.string.restore_requirements_reason_not_installed,
                backup.name ?: authority
            )
            return Item(
                "",
                authority,
                id,
                title,
                reason,
                icon,
                CompatibilityState.Incompatible(reason),
                null,
                invert,
                backup = this
            )
        }
        val requirementForTarget = currentTargets.firstOrNull {
            it.id == requirementFor
        }
        val requirementForComplication = currentComplications.firstOrNull {
            it.id == requirementFor
        }
        val collision = currentRequirements.firstOrNull { it.id == id }
        //If there's a requirement with the same ID but a different authority, prevent adding
        if(collision != null && collision.authority != authority) {
            return null
        }
        @Suppress("CloseRequirement")
        val requirement = Requirement(context, authority, null, invert, provider.packageName)
        val config = requirement.getPluginConfig().firstNotNull()
        requirement.close()
        if(requirementForTarget == null && requirementForComplication == null) {
            val reason = resources.getString(R.string.restore_requirements_reason_not_added)
            return Item(
                id,
                authority,
                provider.packageName,
                config.label,
                reason,
                config.icon,
                CompatibilityState.Incompatible(reason),
                null,
                invert,
                backup = this
            )
        }
        val requirementForLabel = when {
            requirementForTarget != null -> {
                @Suppress("CloseTarget")
                val target = Target(context, requirementForTarget.authority, requirementForTarget.id)
                target.getPluginConfig().firstNotNull().label.also {
                    target.close()
                }
            }
            requirementForComplication != null -> {
                @Suppress("CloseAction")
                val action = Action(
                    context, requirementForComplication.authority, requirementForComplication.id
                )
                action.getPluginConfig().firstNotNull().label.also {
                    action.close()
                }
            }
            else -> null
        }
        //If the requirement is already added with a different ID and it can't be added twice, reject
        val alreadyAdded = !config.allowAddingMoreThanOnce
                && currentRequirements.any { it.authority == authority && it.id != id }
        val compatibilityState = if(alreadyAdded){
            CompatibilityState.Incompatible(resources.getString(R.string.restore_requirements_duplicate))
        }else{
            config.compatibilityState
        }
        val label = backup.name ?: config.description
        val description = if(collision != null){
            label.withCollisionWarning()
        }else label
        return Item(
            id,
            authority,
            provider.packageName,
            config.label,
            description,
            config.icon,
            compatibilityState,
            config.setupActivity,
            invert,
            backup = this,
            requirementForTarget = requirementForTarget,
            requirementForComplication = requirementForComplication,
            requirementForLabel = requirementForLabel
        )
    }

    override fun setupWithConfig(config: RestoreConfig) {
        vmScope.launch {
            this@RestoreRequirementsViewModelImpl.config.emit(config)
        }
    }

    private suspend fun appendAddedRequirementId(id: String) = requirementIdLock.withLock {
        val ids = addedRequirementIds.value
        addedRequirementIds.emit(ids.plus(id))
    }

    override fun onNextClicked() {
        val config = config.value ?: return
        vmScope.launch {
            val directions = when {
                config.shouldRestoreExpandedCustomWidgets -> RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreWidgetsFragment(config)
                config.shouldRestoreSettings -> RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreSettingsFragment(config)
                else -> RestoreRequirementsFragmentDirections.actionRestoreRequirementsFragmentToRestoreCompleteFragment()
            }
            navigation.navigate(directions)
        }
    }

    private fun CharSequence.withCollisionWarning(): CharSequence = SpannableStringBuilder().apply {
        append(resources.getText(R.string.restore_requirements_collision))
        appendLine()
        append(this@withCollisionWarning)
    }

}