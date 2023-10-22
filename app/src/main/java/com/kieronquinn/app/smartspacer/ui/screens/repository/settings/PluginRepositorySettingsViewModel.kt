package com.kieronquinn.app.smartspacer.ui.screens.repository.settings

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class PluginRepositorySettingsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onUpdateCheckEnabledChanged(enabled: Boolean)
    abstract fun onUrlClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val updateCheckEnabled: Boolean
        ): State()
    }

}

class PluginRepositorySettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): PluginRepositorySettingsViewModel(scope) {

    private val enabled = settingsRepository.pluginRepositoryEnabled
    private val updateCheckEnabled = settingsRepository.pluginRepositoryUpdateCheckEnabled

    override val state = combine(
        enabled.asFlow(),
        updateCheckEnabled.asFlow()
    ) { enabled, updates ->
        State.Loaded(enabled, updates)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            this@PluginRepositorySettingsViewModelImpl.enabled.set(enabled)
        }
    }

    override fun onUpdateCheckEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            updateCheckEnabled.set(enabled)
        }
    }

    override fun onUrlClicked() {
        vmScope.launch {
            navigation.navigate(PluginRepositorySettingsFragmentDirections.actionPluginRepositorySettingsFragmentToPluginRepositorySettingsUrlBottomSheetFragment())
        }
    }

}