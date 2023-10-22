package com.kieronquinn.app.smartspacer.ui.screens.setup.plugins

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.setup.container.SetupContainerFragmentDirections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupPluginsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onFinishClicked()

}

class SetupPluginsViewModelImpl(
    private val rootNavigation: RootNavigation,
    scope: CoroutineScope? = null
): SetupPluginsViewModel(scope) {

    override fun onFinishClicked() {
        vmScope.launch {
            rootNavigation.navigate(
                SetupContainerFragmentDirections.actionSetupContainerFragmentToSetupCompleteFragment()
            )
        }
    }

}