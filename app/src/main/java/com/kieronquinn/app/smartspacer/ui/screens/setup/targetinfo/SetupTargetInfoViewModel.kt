package com.kieronquinn.app.smartspacer.ui.screens.setup.targetinfo

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupTargetInfoViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onNextClicked()
    abstract fun onBackPressed(): Boolean

}

class SetupTargetInfoViewModelImpl(
    private val navigation: SetupNavigation,
    private val rootNavigation: RootNavigation,
    scope: CoroutineScope? = null
): SetupTargetInfoViewModel(scope) {

    override fun onNextClicked() {
        vmScope.launch {
            navigation.navigate(
                SetupTargetInfoFragmentDirections.actionSetupTargetInfoFragmentToSetupTargetsFragment()
            )
        }
    }

    override fun onBackPressed(): Boolean {
        vmScope.launch {
            rootNavigation.navigateBack()
        }
        return true
    }

}