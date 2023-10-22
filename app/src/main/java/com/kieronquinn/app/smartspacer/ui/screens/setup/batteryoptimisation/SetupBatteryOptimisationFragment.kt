package com.kieronquinn.app.smartspacer.ui.screens.setup.batteryoptimisation

import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.screens.batteryoptimisation.BatteryOptimisationFragment
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupBatteryOptimisationFragment: BatteryOptimisationFragment(), BackAvailable {

    override val viewModel by viewModel<SetupBatteryOptimisationViewModel>()
    override val showControls = true

    override fun onAcceptabilityChanged(acceptable: Boolean) {
        if(acceptable) {
            whenResumed {
                delay(500L)
                viewModel.moveToNext()
            }
        }
    }

}