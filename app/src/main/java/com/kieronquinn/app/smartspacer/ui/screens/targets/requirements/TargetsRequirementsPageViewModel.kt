package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.RequirementsRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
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

abstract class TargetsRequirementsPageViewModel(
    context: Context,
    databaseRepository: DatabaseRepository,
    scope: CoroutineScope?
): BaseRequirementsViewModelImpl(context, databaseRepository, scope) {

    abstract fun setup(targetId: String, pageType: PageType)

}

class TargetsRequirementsPageViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    private val requirementsRepository: RequirementsRepository,
    scope: CoroutineScope? = null
): TargetsRequirementsPageViewModel(context, databaseRepository, scope) {

    private val targetId = MutableStateFlow<String?>(null)
    private val pageType = MutableStateFlow<PageType?>(null)

    private val pageConfig = flow {
        emit(Pair(targetId.firstNotNull(), pageType.firstNotNull()))
    }

    override fun getRequirements(databaseRepository: DatabaseRepository): Flow<List<Requirement>> {
        return pageConfig.flatMapLatest {
            databaseRepository.getTargetById(it.first).mapLatest { target ->
                target?.getRequirements(it.second)?.mapNotNull { id ->
                    databaseRepository.getRequirementById(id).first()
                } ?: emptyList()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun setup(targetId: String, pageType: PageType) {
        vmScope.launch {
            this@TargetsRequirementsPageViewModelImpl.targetId.emit(targetId)
            this@TargetsRequirementsPageViewModelImpl.pageType.emit(pageType)
        }
    }

    override fun onDeleteClicked(requirement: RequirementHolder) {
        vmScope.launch(Dispatchers.IO) {
            val targetId = targetId.value ?: return@launch
            val type = pageType.value ?: return@launch
            val target = databaseRepository.getTargetById(targetId).first() ?: return@launch
            when(type){
                PageType.ANY -> {
                    target.anyRequirements = target.anyRequirements - requirement.requirement.id
                }
                PageType.ALL -> {
                    target.allRequirements = target.allRequirements - requirement.requirement.id
                }
            }
            requirement.smartspaceRequirement.onDeleted()
            databaseRepository.deleteRequirement(requirement.requirement)
            databaseRepository.updateTarget(target)
        }
    }

    override fun onAddClicked() {
        val pageType = pageType.value ?: return
        val targetId = targetId.value ?: return
        vmScope.launch {
            navigation.navigate(TargetsRequirementsFragmentDirections
                .actionTargetsRequirementsFragmentToTargetsRequirementsAddFragment(targetId, pageType))
        }
    }

    override fun notifyRequirementChange(requirement: RequirementHolder) {
        requirementsRepository.notifyChangeAfterDelay(
            requirement.requirement.id,
            requirement.requirement.authority
        )
    }

    private fun Target.getRequirements(pageType: PageType): Set<String> {
        return when(pageType){
            PageType.ANY -> anyRequirements
            PageType.ALL -> allRequirements
        }
    }

}