package com.kieronquinn.app.smartspacer.ui.screens.setup.complete

import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupCompleteViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onResume()
    abstract fun onCloseClicked()

}

class SetupCompleteViewModelImpl(
    private val settingsRepository: SmartspacerSettingsRepository,
    private val navigation: RootNavigation,
    scope: CoroutineScope? = null
): SetupCompleteViewModel(scope) {

    override fun onResume() {
        vmScope.launch {
            settingsRepository.hasSeenSetup.set(true)
        }
    }

    override fun onCloseClicked() {
        vmScope.launch {
            navigation.navigate(R.id.action_global_settings)
        }
    }

}