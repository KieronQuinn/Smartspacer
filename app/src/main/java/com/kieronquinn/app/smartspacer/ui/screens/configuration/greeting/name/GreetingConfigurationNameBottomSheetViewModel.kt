package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.name

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.targets.GreetingTarget
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class GreetingConfigurationNameBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun setName(name: String)
    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val name: String
        ): State()
    }

}

class GreetingConfigurationNameBottomSheetViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): GreetingConfigurationNameBottomSheetViewModel(scope) {

    private val id = MutableStateFlow<String?>(null)
    private val customName = MutableStateFlow<String?>(null)

    private val settings = id.filterNotNull().flatMapLatest { id ->
        dataRepository.getTargetDataFlow(id, GreetingTarget.TargetData::class.java).map {
            it ?: GreetingTarget.TargetData(settingsRepository.userName.get())
        }
    }.flowOn(Dispatchers.IO)

    override val state = combine(settings, customName) { s, c ->
        State.Loaded(c ?: s.name)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            id.emit(smartspacerId)
        }
    }

    override fun setName(name: String) {
        vmScope.launch {
            customName.emit(name)
        }
    }

    override fun onPositiveClicked() {
        val state = state.value as? State.Loaded ?: return
        vmScope.launch {
            updateData(state.name)
            navigation.navigateBack()
        }
    }

    override fun onNegativeClicked() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    private fun updateData(name: String) {
        val id = id.value ?: return
        vmScope.launch {
            dataRepository.updateTargetData(
                id,
                GreetingTarget.TargetData::class.java,
                TargetDataType.GREETING,
                ::notifyChange
            ) {
                (it ?: GreetingTarget.TargetData()).copy(name = name)
            }
        }
    }

    private fun notifyChange(context: Context, smartspacerId: String) {
        SmartspacerTargetProvider.notifyChange(context, GreetingTarget::class.java, smartspacerId)
    }

}