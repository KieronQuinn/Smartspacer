package com.kieronquinn.app.smartspacer.ui.screens.configuration.blank

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.smartspace.targets.BlankTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.BlankTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BlankTargetConfigurationViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setup(smartspacerId: String)
    abstract fun onShowComplicationsChanged(enabled: Boolean)
    abstract fun onHideIfNoComplicationsChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(val targetData: TargetData): State()
    }

}

class BlankTargetConfigurationViewModelImpl(
    private val dataRepository: DataRepository
): BlankTargetConfigurationViewModel() {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val targetData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java)
    }

    override val state = targetData.mapLatest {
        State.Loaded(it ?: TargetData())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(smartspacerId: String) {
        viewModelScope.launch {
            this@BlankTargetConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onShowComplicationsChanged(enabled: Boolean) {
        updateData {
            copy(showComplications = enabled)
        }
    }

    override fun onHideIfNoComplicationsChanged(enabled: Boolean) {
        updateData {
            copy(hideIfNoComplications = enabled)
        }
    }

    private fun updateData(block: TargetData.() -> TargetData) {
        val smartspacerId = smartspacerId.value ?: return
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.BLANK,
            ::onUpdated
        ) {
            val data = it ?: TargetData()
            block(data)
        }
    }

    private fun onUpdated(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, BlankTarget::class.java, smartspacerId)
    }

}