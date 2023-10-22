package com.kieronquinn.app.smartspacer.ui.screens.setup.landing

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupLandingViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onGetStartedClicked()

}

class SetupLandingViewModelImpl(
    private val rootNavigation: RootNavigation,
    scope: CoroutineScope? = null
): SetupLandingViewModel(scope) {

    override fun onGetStartedClicked() {
        vmScope.launch {
            rootNavigation.navigate(SetupLandingFragmentDirections.actionSetupLandingFragmentToSetupAnalyticsFragment())
        }
    }

}