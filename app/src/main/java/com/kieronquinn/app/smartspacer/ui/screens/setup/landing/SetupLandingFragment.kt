package com.kieronquinn.app.smartspacer.ui.screens.setup.landing

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupLandingBinding
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupLandingFragment: BoundFragment<FragmentSetupLandingBinding>(FragmentSetupLandingBinding::inflate) {

    private val viewModel by viewModel<SetupLandingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyMonet()
        setupGetStarted()
    }

    private fun applyMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.setupLandingGetStarted.run {
            setTextColor(accent)
            overrideRippleColor(accent)
        }
    }

    private fun setupGetStarted() {
        whenResumed {
            binding.setupLandingGetStarted.onClicked().collect {
                viewModel.onGetStartedClicked()
            }
        }
    }

}