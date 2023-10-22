package com.kieronquinn.app.smartspacer.ui.screens.settings.batteryoptimisation

import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsBatteryOptimisationFragment: BatteryOptimisationFragment(), BackAvailable {

    override val viewModel by viewModel<SettingsBatteryOptimisationViewModel>()
    override val showControls = false

}