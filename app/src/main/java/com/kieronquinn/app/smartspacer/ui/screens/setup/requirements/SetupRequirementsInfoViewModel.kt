package com.kieronquinn.app.smartspacer.ui.screens.setup.requirements

import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupRequirementsInfoViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onNextClicked()

}

class SetupRequirementsInfoViewModelImpl(
    private val navigation: SetupNavigation,
    scope: CoroutineScope? = null
): SetupRequirementsInfoViewModel(scope) {

    override fun onNextClicked() {
        vmScope.launch {
            navigation.navigate(
                SetupRequirementsInfoFragmentDirections.actionSetupRequirementsInfoFragmentToSetupExpandedSmartspaceFragment()
            )
        }
    }

}