package com.kieronquinn.app.smartspacer.ui.screens.configuration.date

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DateComplication
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DateComplication.ComplicationData
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
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

abstract class DateComplicationConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setup(smartspacerId: String)
    abstract fun onDateFormatClicked()
    abstract fun onDateFormatChanged(format: String?)

    sealed class State {
        data object Loading: State()
        data class Loaded(val data: ComplicationData): State()
    }

}

class DateComplicationConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): DateComplicationConfigurationViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val complicationData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getActionDataFlow(it, ComplicationData::class.java).map { data ->
            data ?: ComplicationData()
        }.filterNotNull()
    }

    override val state = complicationData.map { data ->
        State.Loaded(data)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(smartspacerId: String) {
        vmScope.launch {
            this@DateComplicationConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onDateFormatClicked() {
        val format = (state.value as? State.Loaded)?.data?.dateFormat ?: ""
        vmScope.launch {
            navigation.navigate(DateComplicationConfigurationFragmentDirections
                .actionDateComplicationConfigurationFragmentToDateComplicationFormatPickerFragment(format))
        }
    }

    override fun onDateFormatChanged(format: String?) {
        val smartspacerId = smartspacerId.value ?: return
        dataRepository.updateActionData(
            smartspacerId,
            ComplicationData::class.java,
            ActionDataType.DATE,
            ::onChanged
        ) {
            val data = it ?: ComplicationData()
            data.copy(dateFormat = format)
        }
    }

    private fun onChanged(context: Context, smartspacerId: String) {
        SmartspacerComplicationProvider.notifyChange(context, DateComplication::class.java, smartspacerId)
    }

}