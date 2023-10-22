package com.kieronquinn.app.smartspacer.ui.screens.setup.complete

import android.os.Bundle
import android.view.View
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupCompleteBinding
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.applyBackgroundTint
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.replaceColour
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupCompleteFragment: BoundFragment<FragmentSetupCompleteBinding>(FragmentSetupCompleteBinding::inflate) {

    private val viewModel by viewModel<SetupCompleteViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCard()
        setupLottie()
        setupClose()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupCard() = with(binding.setupCompleteCard) {
        applyBackgroundTint(monet)
    }

    private fun setupLottie() = with(binding.setupCompleteLottie) {
        val accent = monet.getAccentColor(requireContext(), false)
        replaceColour("Background Circle (Blue)", "**", replaceWith = accent)
        replaceColour("Background(Blue)", "**", replaceWith = accent)
        playAnimation()
    }

    private fun setupClose() = with(binding.setupCompleteClose) {
        val accentColor = monet.getAccentColor(requireContext())
        setTextColor(accentColor)
        whenResumed {
            onClicked().collect {
                viewModel.onCloseClicked()
            }
        }
    }

}