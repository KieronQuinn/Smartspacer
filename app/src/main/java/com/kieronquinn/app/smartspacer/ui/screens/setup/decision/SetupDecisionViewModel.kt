package com.kieronquinn.app.smartspacer.ui.screens.setup.decision

import androidx.navigation.NavDirections
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SetupDecisionViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun navigateTo(directions: NavDirections)

    sealed class State {
        object Loading: State()
        data class Loaded(val directions: NavDirections): State()
    }

}

class SetupDecisionViewModelImpl(
    private val navigation: SetupNavigation,
    private val settingsRepository: SmartspacerSettingsRepository,
    compatibilityRepository: CompatibilityRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null
): SetupDecisionViewModel(scope) {

    override val state = flow {
        val directions = when {
            !notificationRepository.hasNotificationPermission() -> {
                SetupDecisionFragmentDirections.actionSetupDecisionFragmentToSetupNotificationsFragment()
            }
            compatibilityRepository.isEnhancedModeAvailable() && !settingsRepository.enhancedMode.get() -> {
                SetupDecisionFragmentDirections.actionSetupDecisionFragmentToEnhancedModeFragment(true)
            }
            else -> {
                SetupDecisionFragmentDirections.actionSetupDecisionFragmentToSetupTargetsFragment()
            }
        }
        emit(State.Loaded(directions))
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun navigateTo(directions: NavDirections) {
        vmScope.launch {
            navigation.navigate(directions)
        }
    }

}