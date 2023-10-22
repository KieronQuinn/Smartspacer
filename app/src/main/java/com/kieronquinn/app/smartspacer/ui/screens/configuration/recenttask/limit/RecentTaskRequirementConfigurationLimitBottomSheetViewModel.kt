package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.limit

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.RecentTaskRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class RecentTaskRequirementConfigurationLimitBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val limit: StateFlow<Int?>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun onLimitChanged(limit: Int)

    abstract fun onSaveClicked()
    abstract fun onCancelClicked()

}

class RecentTaskRequirementConfigurationLimitBottomSheetViewModelImpl(
    private val navigation: ConfigurationNavigation,
    private val dataRepository: DataRepository,
    scope: CoroutineScope? = null
): RecentTaskRequirementConfigurationLimitBottomSheetViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)
    private val _limit = MutableStateFlow<Int?>(null)

    private val data = id.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java).map { data ->
            data ?: RequirementData()
        }
    }

    override val limit = combine(
        data,
        _limit
    ){ default, custom ->
        custom ?: default.limit ?: 0
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun onLimitChanged(limit: Int) {
        vmScope.launch {
            _limit.emit(limit)
        }
    }

    override fun onSaveClicked() {
        val id = id.value ?: return
        val limit = limit.value?.let {
            if(it == 0) null else it
        }
        dataRepository.updateRequirementData(
            id,
            RequirementData::class.java,
            RequirementDataType.RECENT_TASK,
            ::onDataChanged
        ) {
            val data = it ?: RequirementData()
            data.copy(limit = limit)
        }
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onCancelClicked() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    private fun onDataChanged(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider.notifyChange(
            context, RecentTaskRequirement::class.java, smartspacerId
        )
    }

}