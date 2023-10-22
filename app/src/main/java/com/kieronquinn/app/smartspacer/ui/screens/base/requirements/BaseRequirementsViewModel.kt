package com.kieronquinn.app.smartspacer.ui.screens.base.requirements

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.model.smartspace.Requirement as SmartspaceRequirement

abstract class BaseRequirementsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onDeleteClicked(requirement: RequirementHolder)
    abstract fun onAddClicked()
    abstract fun notifyRequirementChange(requirement: RequirementHolder)
    abstract fun invertRequirement(requirement: RequirementHolder)
    abstract fun reload()

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<RequirementHolder>): State()
    }

    /**
     *  Holds a cached state of a given requirement, to prevent having to re-load from the provider
     *  every time it's needed
     */
    data class RequirementHolder(
        val requirement: Requirement,
        val smartspaceRequirement: SmartspaceRequirement,
        val icon: Icon,
        val label: CharSequence,
        val description: CharSequence,
        val configurationIntent: Intent?,
        val compatibilityState: CompatibilityState
    )

    enum class PageType {
        ANY, ALL
    }

}

abstract class BaseRequirementsViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    scope: CoroutineScope?
): BaseRequirementsViewModel(scope) {

    private val reloadBus = MutableStateFlow<Long?>(null)

    abstract fun getRequirements(
        databaseRepository: DatabaseRepository
    ): Flow<List<Requirement>>

    private val items by lazy {
        reloadBus.filterNotNull().flatMapLatest {
            getRequirements(databaseRepository)
        }
    }

    override val state by lazy {
        items.mapLatest {
            it.map { requirement ->
                requirement.toRequirementHolder(context)
            }.let { items ->
                State.Loaded(items)
            }
        }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)
    }

    override fun invertRequirement(requirement: RequirementHolder) {
        val invertedRequirement = requirement.requirement.let {
            it.copy(invert = !it.invert)
        }
        vmScope.launch {
            databaseRepository.addRequirement(invertedRequirement)
            reload()
        }
    }

    private suspend fun Requirement.toRequirementHolder(context: Context): RequirementHolder {
        val requirement = SmartspaceRequirement(context, authority, id, invert, packageName)
        val config = requirement.getPluginConfig().firstNotNull()
        return RequirementHolder(
            this,
            requirement,
            config.icon,
            config.label,
            config.description,
            config.configActivity,
            config.compatibilityState
        )
    }

    override fun reload() {
        vmScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

}