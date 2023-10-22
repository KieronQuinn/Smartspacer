package com.kieronquinn.app.smartspacer.ui.screens.setup.requirements

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupRequirementsInfoBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.toArgb
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupRequirementsInfoFragment: BoundFragment<FragmentSetupRequirementsInfoBinding>(FragmentSetupRequirementsInfoBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupRequirementsInfoViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        setupMonet()
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupRequirementsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupRequirementsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupRequirementsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupRequirementsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

    private fun setupMonet() {
        val tabBackground = monet.getMonetColors().accent1[600]?.toArgb()
            ?: monet.getAccentColor(requireContext(), false)
        val accent = monet.getAccentColor(requireContext())
        binding.setupRequirementsInfoAny.backgroundTintList = ColorStateList.valueOf(tabBackground)
        binding.setupRequirementsInfoAny.setSelectedTabIndicatorColor(accent)
        binding.setupRequirementsInfoAll.backgroundTintList = ColorStateList.valueOf(tabBackground)
        binding.setupRequirementsInfoAll.setSelectedTabIndicatorColor(accent)
    }
}