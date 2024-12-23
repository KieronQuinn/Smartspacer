package com.kieronquinn.app.smartspacer.ui.screens.configuration.widgettarget.configuration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget.TargetData
import com.kieronquinn.app.smartspacer.components.smartspace.targets.WidgetTarget.TargetData.Padding
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

abstract class WidgetTargetConfigurationViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun onPaddingChanged(padding: Padding)
    abstract fun onRoundChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val data: TargetData
        ): State()
    }

}

class WidgetTargetConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val smartspacerId: String
): WidgetTargetConfigurationViewModel() {

    override val state = dataRepository.getTargetDataFlow(smartspacerId, TargetData::class.java)
        .mapNotNull { State.Loaded(it ?: return@mapNotNull null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun onPaddingChanged(padding: Padding) {
        val current = (state.value as? State.Loaded)?.data ?: return
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.WIDGET,
            ::onUpdate
        ) {
            current.copy(padding = padding)
        }
    }

    override fun onRoundChanged(enabled: Boolean) {
        val current = (state.value as? State.Loaded)?.data ?: return
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.WIDGET,
            ::onUpdate
        ) {
            current.copy(rounded = enabled)
        }
    }

    private fun onUpdate(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, WidgetTarget::class.java, smartspacerId)
    }

}