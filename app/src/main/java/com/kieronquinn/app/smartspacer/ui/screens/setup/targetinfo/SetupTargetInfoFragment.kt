package com.kieronquinn.app.smartspacer.ui.screens.setup.targetinfo

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupTargetInfoBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.ProvidesBack
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.setShadowEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupTargetInfoFragment: BoundFragment<FragmentSetupTargetInfoBinding>(FragmentSetupTargetInfoBinding::inflate), BackAvailable, ProvidesBack {

    private val viewModel by viewModel<SetupTargetInfoViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        setupExampleTargetOne()
        setupExampleTargetTwo()
    }

    override fun onBackPressed(): Boolean {
        return viewModel.onBackPressed()
    }

    private fun setupExampleTargetOne() = with(binding.setupTargetInfoExample1) {
        smartspacePageFeatureBasicTitle.smartspaceViewTitle
            .setText(R.string.setup_targets_info_example_one_title)
        smartspacePageFeatureBasicTitle.smartspaceViewTitle.setShadowEnabled(false)
        smartspacePageFeatureBasicSubtitle.smartspacePageSubtitleText
            .setText(R.string.setup_targets_info_example_one_subtitle)
        smartspacePageFeatureBasicSubtitle.smartspacePageSubtitleText.setShadowEnabled(false)
        smartspacePageFeatureBasicSubtitle.smartspacePageSubtitleIcon
            .setImageResource(R.drawable.ic_target_example_one)
        smartspacePageFeatureBasicSubtitle.smartspacePageSubtitleIcon.applyShadow = false
        smartspacePageCommuteTimeImage.setImageResource(R.drawable.example_target_image)
    }

    private fun setupExampleTargetTwo() = with(binding.setupTargetInfoExample2) {
        smartspacePageTemplateBasicTitle.smartspaceViewTitle
            .setText(R.string.setup_targets_info_example_two_title)
        smartspacePageTemplateBasicTitle.smartspaceViewTitle.setShadowEnabled(false)
        smartspacePageTemplateBasicSubtitle.smartspacePageSubtitleText
            .setText(R.string.setup_targets_info_example_two_subtitle)
        smartspacePageTemplateBasicSubtitle.smartspacePageSubtitleText.setShadowEnabled(false)
        smartspacePageTemplateBasicSubtitle.smartspacePageSubtitleIcon
            .setImageResource(R.drawable.ic_target_example_two)
        smartspacePageTemplateBasicSubtitle.smartspacePageSubtitleIcon.applyShadow = false
        smartspaceViewListItem1.setText(R.string.setup_targets_info_example_two_item_1)
        smartspaceViewListItem1.setShadowEnabled(false)
        smartspaceViewListItem2.setText(R.string.setup_targets_info_example_two_item_2)
        smartspaceViewListItem2.setShadowEnabled(false)
        smartspaceViewListItem3.setText(R.string.setup_targets_info_example_two_item_3)
        smartspaceViewListItem3.setShadowEnabled(false)
        smartspacePageTemplateBasicSupplemental.root.isVisible = false
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupTargetsControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupTargetsControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupTargetsControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupTargetsControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

}