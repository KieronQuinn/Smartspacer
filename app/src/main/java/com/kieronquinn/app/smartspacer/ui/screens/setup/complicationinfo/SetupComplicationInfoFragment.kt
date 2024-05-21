package com.kieronquinn.app.smartspacer.ui.screens.setup.complicationinfo

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupComplicationInfoBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.getAttrColor
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.setShadowEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupComplicationInfoFragment: BoundFragment<FragmentSetupComplicationInfoBinding>(FragmentSetupComplicationInfoBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupComplicationInfoViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        setupExampleComplicationOne()
        setupExampleComplicationTwo()
    }

    private fun setupExampleComplicationOne() = with(binding.setupComplicationInfoExample1) {
        smartspacePageSubtitleIcon.setImageResource(R.drawable.ic_complication_gmail)
        smartspacePageSubtitleIcon.applyShadow = false
        smartspacePageSubtitleIcon.imageTintList = ColorStateList.valueOf(
            requireContext().getAttrColor(android.R.attr.textColorPrimary)
        )
        smartspacePageSubtitleText.setText(R.string.setup_complications_example_gmail)
        smartspacePageSubtitleText.setShadowEnabled(false)
    }

    private fun setupExampleComplicationTwo() = with(binding.setupComplicationInfoExample2) {
        smartspacePageSubtitleIcon.setImageResource(R.drawable.ic_widget_smartspacer_preview_weather)
        smartspacePageSubtitleIcon.applyShadow = false
        smartspacePageSubtitleText.setText(R.string.setup_complications_example_weather)
        smartspacePageSubtitleText.setShadowEnabled(false)
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupComplicationsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupComplicationsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupComplicationsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupComplicationsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

}