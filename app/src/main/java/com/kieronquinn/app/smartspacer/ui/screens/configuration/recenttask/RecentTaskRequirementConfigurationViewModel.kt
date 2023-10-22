package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement.RequirementData
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class RecentTaskRequirementConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun onSelectedAppClicked()
    abstract fun onLimitClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val selectedAppName: CharSequence?,
            val limit: Int?
        ): State()
    }

}

class RecentTaskRequirementConfigurationViewModelImpl(
    private val navigation: ConfigurationNavigation,
    context: Context,
    dataRepository: DataRepository,
    scope: CoroutineScope? = null
): RecentTaskRequirementConfigurationViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java).map { data ->
            data ?: RequirementData()
        }
    }

    override val state = data.mapLatest {
        val appName = it.appPackageName?.let { packageName ->
            context.packageManager.getPackageLabel(packageName)
        }
        State.Loaded(appName, it.limit)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun onSelectedAppClicked() {
        val id = id.value ?: return
        vmScope.launch {
            navigation.navigate(RecentTaskRequirementConfigurationFragmentDirections.actionRecentTaskRequirementConfigurationFragmentToRecentTaskRequirementConfigurationAppPickerFragment(id))
        }
    }

    override fun onLimitClicked() {
        val id = id.value ?: return
        vmScope.launch {
            navigation.navigate(RecentTaskRequirementConfigurationFragmentDirections.actionRecentTaskRequirementConfigurationFragmentToRecentTaskRequirementConfigurationLimitBottomSheetFragment(id))
        }
    }

}