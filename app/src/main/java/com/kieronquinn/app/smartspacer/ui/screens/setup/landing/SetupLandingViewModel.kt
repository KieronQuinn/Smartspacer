package com.kieronquinn.app.smartspacer.ui.screens.setup.landing

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupLandingViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onGetStartedClicked()
    abstract fun showDialogsIfNeeded()

}

class SetupLandingViewModelImpl(
    private val rootNavigation: RootNavigation,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    scope: CoroutineScope? = null
): SetupLandingViewModel(scope) {

    override fun onGetStartedClicked() {
        vmScope.launch {
            rootNavigation.navigate(SetupLandingFragmentDirections.actionSetupLandingFragmentToSetupAnalyticsFragment())
        }
    }

    override fun showDialogsIfNeeded() {
        viewModelScope.launch {
            if (shizukuServiceRepository.shouldShowVersionOutdatedDialog()) {
                rootNavigation.navigate(R.id.action_global_shizukuOutdatedBottomSheetFragment2)
            }
        }
    }

}