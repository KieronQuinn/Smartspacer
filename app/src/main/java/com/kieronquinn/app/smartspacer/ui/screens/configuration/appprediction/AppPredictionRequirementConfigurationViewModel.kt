package com.kieronquinn.app.smartspacer.ui.screens.configuration.appprediction

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.AppPredictionRequirement.AppPredictionRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository
import com.kieronquinn.app.smartspacer.repositories.PackageRepository.ListAppsApp
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class AppPredictionRequirementConfigurationViewModel(
    scope: CoroutineScope?
): BaseViewModel(scope) {

    abstract val closeBus: Flow<Unit>
    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String

    abstract fun onAppClicked(id: String, item: ListAppsApp)

    sealed class State {
        object Loading: State()
        data class Loaded(val apps: List<ListAppsApp>): State()
    }

}

class AppPredictionRequirementConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    packageRepository: PackageRepository,
    scope: CoroutineScope? = null
): AppPredictionRequirementConfigurationViewModel(scope) {

    private val allApps = flow {
        emit(packageRepository.getInstalledApps())
    }.flowOn(Dispatchers.IO)

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val closeBus = MutableSharedFlow<Unit>()

    override val showSearchClear = searchTerm.map { it.isNotBlank() }
        .stateIn(vmScope, SharingStarted.Eagerly, false)

    override val state = combine(allApps, searchTerm) { all, term ->
        State.Loaded(all.filter {
            it.label.contains(term, true) || it.packageName.contains(term, true)
        })
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun onAppClicked(id: String, item: ListAppsApp) {
        dataRepository.updateRequirementData(
            id,
            AppPredictionRequirementData::class.java,
            RequirementDataType.APP_PREDICTION,
            ::onDataAdded
        ) {
            AppPredictionRequirementData(id, item.packageName)
        }
    }

    private fun onDataAdded(context: Context, smartspacerId: String) {
        vmScope.launch {
            SmartspacerRequirementProvider.notifyChange(
                context, AppPredictionRequirement::class.java, smartspacerId
            )
            closeBus.emit(Unit)
        }
    }

}