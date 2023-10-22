package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.limit

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentRecentTaskRequirementConfigurationLimitBottomSheetBinding
import com.kieronquinn.app.smartspacer.ui.base.BaseBottomSheetFragment
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import kotlinx.coroutines.flow.drop
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecentTaskRequirementConfigurationLimitBottomSheetFragment: BaseBottomSheetFragment<FragmentRecentTaskRequirementConfigurationLimitBottomSheetBinding>(FragmentRecentTaskRequirementConfigurationLimitBottomSheetBinding::inflate) {

    private val viewModel by viewModel<RecentTaskRequirementConfigurationLimitBottomSheetViewModel>()
    private val args by navArgs<RecentTaskRequirementConfigurationLimitBottomSheetFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMonet()
        setupSlider()
        setupLimit()
        setupInsets()
        setupPositive()
        setupNegative()
        viewModel.setupWithId(args.id)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        val primary = monet.getPrimaryColor(requireContext())
        binding.recentTaskRequirementConfigurationLimitPositive.setTextColor(accent)
        binding.recentTaskRequirementConfigurationLimitNegative.setTextColor(accent)
        binding.recentTaskRequirementConfigurationLimitPositive.overrideRippleColor(primary)
        binding.recentTaskRequirementConfigurationLimitNegative.overrideRippleColor(primary)
        binding.recentTaskRequirementConfigurationLimitSlider.applyMonet()
    }

    private fun setupLimit() {
        handleLimit(viewModel.limit.value)
        whenResumed {
            viewModel.limit.collect {
                handleLimit(it)
            }
        }
    }

    private fun handleLimit(limit: Int?) = with(binding.recentTaskRequirementConfigurationLimitSlider) {
        isEnabled = limit != null
        value = limit?.toFloat() ?: 0f
    }

    private fun setupSlider() = with(binding.recentTaskRequirementConfigurationLimitSlider) {
        setLabelFormatter {
            if(it == 0f){
                getString(R.string.requirement_recent_apps_configuration_limit_content_unlimited)
            }else{
                it.toInt().toString()
            }
        }
        whenResumed {
            onChanged().drop(1).collect {
                viewModel.onLimitChanged(it.toInt())
            }
        }
    }

    private fun setupPositive() = with(binding.recentTaskRequirementConfigurationLimitPositive) {
        whenResumed {
            onClicked().collect {
                viewModel.onSaveClicked()
            }
        }
    }

    private fun setupNegative() = with(binding.recentTaskRequirementConfigurationLimitNegative) {
        whenResumed {
            onClicked().collect {
                viewModel.onCancelClicked()
            }
        }
    }

    private fun setupInsets() = with(binding.root) {
        val padding = resources.getDimension(R.dimen.margin_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            updatePadding(bottom = bottomInset + padding)
        }
    }

}