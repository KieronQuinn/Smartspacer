package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements.add

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

abstract class ComplicationsRequirementsAddViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setup(id: String, pageType: PageType)
    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String
    abstract fun onExpandClicked(item: Item.App)
    abstract fun dismiss()

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>): State()
    }

    sealed class Item(val type: Type) {
        data class App(
            val packageName: String,
            val label: CharSequence,
            var isExpanded: Boolean = false
        ): Item(Type.APP)

        data class Requirement(
            val packageName: String,
            val authority: String,
            val id: String,
            val label: CharSequence,
            val description: CharSequence,
            val icon: Icon,
            val compatibilityState: CompatibilityState,
            val setupIntent: Intent?,
            var isLastRequirement: Boolean = false
        ): Item(Type.REQUIREMENT)

        enum class Type {
            APP, REQUIREMENT
        }
    }

}

class ComplicationsRequirementsAddViewModelImpl(
    context: Context,
    private val requirementsRepository: RequirementsRepository,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): ComplicationsRequirementsAddViewModel(scope) {

    private val packageManager = context.packageManager
    private val expandBus = MutableStateFlow(System.currentTimeMillis())
    private val pageConfig = MutableStateFlow<Pair<String, PageType>?>(null)

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val showSearchClear = searchTerm.mapLatest { it.isNotEmpty() }
        .stateIn(vmScope, SharingStarted.Eagerly, searchTerm.value.isNotEmpty())

    private val requirements = flow {
        val localConfig = pageConfig.firstNotNull()
        val id = localConfig.first
        val type = localConfig.second
        val action = databaseRepository.getActionById(id).first() ?: run {
            emit(emptyList())
            return@flow
        }
        val existingRequirements = if(type == PageType.ANY){
            action.anyRequirements
        }else{
            action.allRequirements
        }.mapNotNull {
            databaseRepository.getRequirementById(it).first()
        }.map {
            it.authority
        }
        val newRequirements = requirementsRepository.getAllRequirements().mapNotNull { req ->
            val config = req.getPluginConfig().firstNotNull()
            if(!config.allowAddingMoreThanOnce && existingRequirements.contains(req.authority)){
                return@mapNotNull null
            }
            Item.Requirement(
                req.sourcePackage,
                req.authority,
                UUID.randomUUID().toString(),
                config.label,
                config.description,
                config.icon,
                config.compatibilityState,
                config.setupActivity
            ).also {
                req.close()
            }
        }.sortedBy {
            it.label.toString().lowercase()
        }
        val groupedRequirements = newRequirements.groupBy { it.packageName }.mapKeys {
            val label = packageManager.getPackageLabel(it.key) ?: ""
            Item.App(it.key, label)
        }.toList().sortedBy { it.first.label.toString().lowercase() }
        emit(groupedRequirements)
    }.flowOn(dispatcher)

    override val state = combine(requirements, searchTerm, expandBus) { t, s, _ ->
        val requirementList = ArrayList<Item>()
        val searchTerm = s.trim()
        t.forEach {
            //If the app's name matches the term, return all its requirements
            if(it.first.label.contains(searchTerm, true)) {
                requirementList.add(it.first)
                if(it.first.isExpanded) {
                    it.second.forEachIndexed { index, requirement ->
                        requirement.isLastRequirement = index == it.second.size - 1
                        requirementList.add(requirement)
                    }
                }
                return@forEach
            }
            //Otherwise, look for matching requirement labels or descriptions for this app
            val matchingRequirements = it.second.filter { requirement ->
                requirement.label.contains(searchTerm, true) ||
                        requirement.description.contains(searchTerm, true)
            }
            if(matchingRequirements.isEmpty()) return@forEach
            requirementList.add(it.first)
            if(it.first.isExpanded) {
                matchingRequirements.forEachIndexed { index, requirement ->
                    requirement.isLastRequirement = index == matchingRequirements.size - 1
                    requirementList.add(requirement)
                }
            }
        }
        State.Loaded(requirementList)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(id: String, pageType: PageType) {
        vmScope.launch {
            pageConfig.emit(Pair(id, pageType))
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun onExpandClicked(item: Item.App) {
        item.isExpanded = !item.isExpanded
        vmScope.launch {
            expandBus.emit(System.currentTimeMillis())
        }
    }

    override fun dismiss() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

}