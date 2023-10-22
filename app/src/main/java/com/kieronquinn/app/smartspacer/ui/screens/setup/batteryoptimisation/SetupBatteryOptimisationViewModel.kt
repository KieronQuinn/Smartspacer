package com.kieronquinn.app.smartspacer.ui.screens.setup.batteryoptimisation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.BatteryOptimisationRepository
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationViewModelImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class SetupBatteryOptimisationViewModel(
    context: Context,
    batteryOptimisationRepository: BatteryOptimisationRepository,
    setupNavigation: SetupNavigation,
    scope: CoroutineScope?
): BatteryOptimisationViewModelImpl(context, batteryOptimisationRepository, setupNavigation, scope)

class SetupBatteryOptimisationViewModelImpl(
    context: Context,
    batteryOptimisationRepository: BatteryOptimisationRepository,
    private val setupNavigation: SetupNavigation,
    scope: CoroutineScope? = null
): SetupBatteryOptimisationViewModel(context, batteryOptimisationRepository, setupNavigation, scope) {

    override fun moveToNext() {
        viewModelScope.launch {
            setupNavigation.navigate(SetupBatteryOptimisationFragmentDirections.actionSetupBatteryOptimisationFragmentToSetupWidgetFragment())
        }
    }

}