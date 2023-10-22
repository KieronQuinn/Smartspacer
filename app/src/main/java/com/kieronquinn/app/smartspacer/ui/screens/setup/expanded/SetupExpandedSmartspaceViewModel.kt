package com.kieronquinn.app.smartspacer.ui.screens.setup.expanded

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupExpandedSmartspaceViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onNextClicked()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onExpandedOpenModeChanged(enabled: Boolean)

    data class State(
        val expandedEnabled: Boolean,
        val expandedOpenEnabled: Boolean
    )

}

class SetupExpandedSmartspaceViewModelImpl(
    private val navigation: SetupNavigation,
    settings: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): SetupExpandedSmartspaceViewModel(scope) {

    private val expandedEnabled = settings.expandedModeEnabled
    private val expandedOpenModeHome = settings.expandedOpenModeHome
    private val expandedOpenModeLock = settings.expandedOpenModeLock

    override val state = combine(
        expandedEnabled.asFlow(),
        expandedOpenModeHome.asFlow(),
        expandedOpenModeLock.asFlow()
    ) { enabled, home, lock ->
        getState(enabled, home, lock)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        getState(
            expandedEnabled.getSync(),
            expandedOpenModeHome.getSync(),
            expandedOpenModeLock.getSync()
        )
    )

    private fun getState(enabled: Boolean, home: ExpandedOpenMode, lock: ExpandedOpenMode): State {
        //Open enabled is compressed into one, and must be at least enabled for both
        val expandedOpenEnabled = home != ExpandedOpenMode.NEVER && lock != ExpandedOpenMode.NEVER
        return State(enabled, expandedOpenEnabled)
    }

    override fun onNextClicked() {
        vmScope.launch {
            navigation.navigate(
                SetupExpandedSmartspaceFragmentDirections.actionSetupExpandedSmartspaceFragmentToSetupPluginsFragment()
            )
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            expandedEnabled.set(enabled)
        }
    }

    override fun onExpandedOpenModeChanged(enabled: Boolean) {
        vmScope.launch {
            val mode = if(enabled) ExpandedOpenMode.IF_HAS_EXTRAS else ExpandedOpenMode.NEVER
            expandedOpenModeHome.set(mode)
            expandedOpenModeLock.set(mode)
        }
    }

}