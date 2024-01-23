package com.kieronquinn.app.smartspacer.ui.screens.configuration.flashlight

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.targets.FlashlightTarget
import com.kieronquinn.app.smartspacer.components.smartspace.targets.FlashlightTarget.TargetData
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.hasLightSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class FlashlightTargetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setup(smartspacerId: String)
    abstract fun onRecommendedChanged(enabled: Boolean)

    sealed class State {
        data object Loading: State()
        data class Loaded(val data: TargetData, val compatible: Boolean): State()
    }

}

class FlashlightTargetConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    context: Context,
    scope: CoroutineScope? = null
): FlashlightTargetConfigurationViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val targetData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getTargetDataFlow(it, TargetData::class.java)
    }

    private val hasLightSensor = flow {
        emit(context.hasLightSensor())
    }

    override val state = combine(
        targetData,
        hasLightSensor
    ) { data, compatible ->
        State.Loaded(data ?: TargetData(), compatible)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(smartspacerId: String) {
        vmScope.launch {
            this@FlashlightTargetConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onRecommendedChanged(enabled: Boolean) {
        val smartspacerId = smartspacerId.value ?: return
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.FLASHLIGHT,
            ::onChanged
        ) {
            TargetData(enabled)
        }
    }

    private fun onChanged(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, FlashlightTarget::class.java, smartspacerId)
    }

}