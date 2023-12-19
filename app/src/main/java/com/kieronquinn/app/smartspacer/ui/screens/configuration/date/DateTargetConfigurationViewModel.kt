package com.kieronquinn.app.smartspacer.ui.screens.configuration.date

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DateTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DateTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class DateTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setup(smartspacerId: String)
    abstract fun onDateFormatClicked()
    abstract fun onDateFormatChanged(format: String?)

    sealed class State {
        data object Loading: State()
        data class Loaded(val data: TargetData): State()
    }

}

class DateTargetConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): DateTargetConfigurationViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val targetData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java).map { data ->
            data ?: TargetData()
        }.filterNotNull()
    }

    override val state = targetData.map { data ->
        State.Loaded(data)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(smartspacerId: String) {
        vmScope.launch {
            this@DateTargetConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onDateFormatClicked() {
        val format = (state.value as? State.Loaded)?.data?.dateFormat ?: ""
        vmScope.launch {
            navigation.navigate(DateTargetConfigurationFragmentDirections
                .actionDateTargetConfigurationFragmentToDateTargetFormatPickerFragment(format))
        }
    }

    override fun onDateFormatChanged(format: String?) {
        val smartspacerId = smartspacerId.value ?: return
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.DATE,
            ::onChanged
        ) {
            val data = it ?: TargetData()
            data.copy(dateFormat = format)
        }
    }

    private fun onChanged(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, DateTarget::class.java, smartspacerId)
    }

}