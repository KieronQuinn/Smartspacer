package com.kieronquinn.app.smartspacer.ui.screens.complications.requirements

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.model.database.Action as Complication

abstract class ComplicationsRequirementsPageViewModel(
    context: Context,
    databaseRepository: DatabaseRepository,
    scope: CoroutineScope?
): BaseRequirementsViewModelImpl(context, databaseRepository, scope) {

    abstract fun setup(requirementId: String, pageType: PageType)

}

class ComplicationsRequirementsPageViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    private val requirementsRepository: RequirementsRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): ComplicationsRequirementsPageViewModel(context, databaseRepository, scope) {

    private val requirementId = MutableStateFlow<String?>(null)
    private val pageType = MutableStateFlow<PageType?>(null)

    private val pageConfig = flow {
        emit(Pair(requirementId.firstNotNull(), pageType.firstNotNull()))
    }

    override fun getRequirements(databaseRepository: DatabaseRepository): Flow<List<Requirement>> {
        return pageConfig.flatMapLatest {
            databaseRepository.getActionById(it.first).mapLatest { complication ->
                complication?.getRequirements(it.second)?.mapNotNull { id ->
                    databaseRepository.getRequirementById(id).first()
                } ?: emptyList()
            }
        }.flowOn(dispatcher)
    }

    override fun setup(requirementId: String, pageType: PageType) {
        vmScope.launch {
            this@ComplicationsRequirementsPageViewModelImpl.requirementId.emit(requirementId)
            this@ComplicationsRequirementsPageViewModelImpl.pageType.emit(pageType)
        }
    }

    override fun onDeleteClicked(requirement: RequirementHolder) {
        vmScope.launch(dispatcher) {
            val requirementId = requirementId.value ?: return@launch
            val type = pageType.value ?: return@launch
            val complication = databaseRepository.getActionById(requirementId).first()
                ?: return@launch
            when(type){
                PageType.ANY -> {
                    complication.anyRequirements =
                        complication.anyRequirements - requirement.requirement.id
                }
                PageType.ALL -> {
                    complication.allRequirements =
                        complication.allRequirements - requirement.requirement.id
                }
            }
            requirement.smartspaceRequirement.onDeleted()
            databaseRepository.deleteRequirement(requirement.requirement)
            databaseRepository.updateAction(complication)
        }
    }

    override fun notifyRequirementChange(requirement: RequirementHolder) {
        requirementsRepository.notifyChangeAfterDelay(
            requirement.requirement.id,
            requirement.requirement.authority
        )
    }

    override fun onAddClicked() {
        val pageType = pageType.value ?: return
        val complicationId = requirementId.value ?: return
        vmScope.launch {
            navigation.navigate(ComplicationsRequirementsFragmentDirections
                .actionComplicationsRequirementsFragmentToComplicationsRequirementsAddFragment(
                    complicationId, pageType))
        }
    }

    private fun Complication.getRequirements(pageType: PageType): Set<String> {
        return when(pageType){
            PageType.ANY -> anyRequirements
            PageType.ALL -> allRequirements
        }
    }

}