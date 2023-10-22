package com.kieronquinn.app.smartspacer.ui.screens.setup.complicationinfo

import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupComplicationInfoViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onNextClicked()

}

class SetupComplicationInfoViewModelImpl(
    private val navigation: SetupNavigation,
    scope: CoroutineScope? = null
): SetupComplicationInfoViewModel(scope) {

    override fun onNextClicked() {
        vmScope.launch {
            navigation.navigate(
                SetupComplicationInfoFragmentDirections.actionSetupComplicationInfoFragmentToSetupComplicationsFragment()
            )
        }
    }

}