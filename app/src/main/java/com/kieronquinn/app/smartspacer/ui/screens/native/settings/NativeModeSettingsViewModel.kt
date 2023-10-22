package com.kieronquinn.app.smartspacer.ui.screens.native.settings

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.isAtLeastU
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class NativeModeSettingsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onCountLimitClicked(isFromSettings: Boolean)
    abstract fun onHideIncompatibleChanged(enabled: Boolean)
    abstract fun onUseSplitSmartspaceChanged(enabled: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val targetCountLimit: TargetCountLimit,
            val hideIncompatibleTargets: Boolean,
            val supportsSplitSmartspace: Boolean,
            val useSplitSmartspace: Boolean
        ): State()
    }

}

class NativeModeSettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): NativeModeSettingsViewModel(scope) {

    private val hideIncompatible = settingsRepository.nativeHideIncompatible
    private val useSplitSmartspace = settingsRepository.nativeUseSplitSmartspace

    override val state = combine(
        settingsRepository.nativeTargetCountLimit.asFlow(),
        hideIncompatible.asFlow(),
        useSplitSmartspace.asFlow()
    ) { countLimit, hideIncompatible, splitSmartspace ->
        State.Loaded(countLimit, hideIncompatible, isAtLeastU(), splitSmartspace)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onCountLimitClicked(isFromSettings: Boolean) {
        vmScope.launch {
            navigation.navigate(NativeModeSettingsFragmentDirections.actionNativeModeSettingsFragmentToNativeModePageLimitFragment(isFromSettings))
        }
    }

    override fun onHideIncompatibleChanged(enabled: Boolean) {
        vmScope.launch {
            hideIncompatible.set(enabled)
        }
    }

    override fun onUseSplitSmartspaceChanged(enabled: Boolean) {
        vmScope.launch {
            useSplitSmartspace.set(enabled)
        }
    }

}