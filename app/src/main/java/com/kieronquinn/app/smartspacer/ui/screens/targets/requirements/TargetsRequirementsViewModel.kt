package com.kieronquinn.app.smartspacer.ui.screens.targets.requirements

import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.base.requirements.BaseRequirementsViewModel.PageType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class TargetsRequirementsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun setCurrentPage(currentPage: Int)
    abstract fun getCurrentPage(): Int

    abstract fun addRequirement(
        targetId: String,
        type: PageType,
        authority: String,
        id: String,
        packageName: String
    )

}

class TargetsRequirementsViewModelImpl(
    private val databaseRepository: DatabaseRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): TargetsRequirementsViewModel(scope) {

    private var currentPage = 0

    override fun setCurrentPage(currentPage: Int) {
        this.currentPage = currentPage
    }

    override fun getCurrentPage(): Int = currentPage

    override fun addRequirement(
        targetId: String,
        type: PageType,
        authority: String,
        id: String,
        packageName: String
    ) {
        vmScope.launch(dispatcher) {
            val target = databaseRepository.getTargetById(targetId).first() ?: return@launch
            @Suppress("CloseRequirement")
            val requirement = Requirement(id, authority, packageName, false)
            databaseRepository.addRequirement(requirement)
            when(type){
                PageType.ALL -> target.allRequirements = target.allRequirements + requirement.id
                PageType.ANY -> target.anyRequirements = target.anyRequirements + requirement.id
            }
            databaseRepository.updateTarget(target)
        }
    }

}