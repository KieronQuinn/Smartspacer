package com.kieronquinn.app.smartspacer.ui.screens.setup.expanded

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentSetupExpandedBinding
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.screens.setup.expanded.SetupExpandedSmartspaceViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.onApplyInsets
import com.kieronquinn.app.smartspacer.utils.extensions.onChanged
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class SetupExpandedSmartspaceFragment: BoundFragment<FragmentSetupExpandedBinding>(FragmentSetupExpandedBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SetupExpandedSmartspaceViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        setupState()
        setupSetting()
        setupSwitch()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        binding.setupExpandedSwitch.isChecked = state.expandedEnabled
        binding.setupExpandedOpenEnabled.itemSettingsSwitchSwitch.isChecked =
            state.expandedOpenEnabled
        binding.setupExpandedOpenEnabled.root.isEnabled = state.expandedEnabled
        binding.setupExpandedOpenEnabled.root.alpha = if(state.expandedEnabled) 1f else 0.5f
        binding.setupExpandedOpenEnabled.itemSettingsSwitchSwitch.isEnabled =
            state.expandedEnabled
    }

    private fun setupSwitch() {
        whenResumed {
            binding.setupExpandedSwitch.onClicked().collect {
                viewModel.onEnabledChanged(!binding.setupExpandedSwitch.isChecked)
            }
        }
    }

    private fun setupSetting() = with(binding.setupExpandedOpenEnabled) {
        itemSettingsSwitchTitle.setText(R.string.setup_expanded_open_title)
        itemSettingsSwitchContent.setText(R.string.setup_expanded_open_content)
        itemSettingsSwitchContent.isVisible = true
        itemSettingsSwitchSpace.isVisible = true
        itemSettingsSwitchIcon.isVisible = false
        itemSettingsSwitchSwitch.setOnCheckedChangeListener(null)
        itemSettingsSwitchSwitch.applyMonet()
        whenResumed {
            root.onClicked().collect {
                if(!root.isEnabled) return@collect
                itemSettingsSwitchSwitch.toggle()
            }
        }
        whenResumed {
            itemSettingsSwitchSwitch.onChanged().collect {
                if(!root.isEnabled) return@collect
                viewModel.onExpandedOpenModeChanged(it)
            }
        }
    }

    private fun setupControls() {
        val background = monet.getBackgroundColorSecondary(requireContext())
            ?: monet.getBackgroundColor(requireContext())
        binding.setupExpandedControls.backgroundTintList = ColorStateList.valueOf(background)
        binding.setupExpandedControlsNext.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
        val normalPadding = resources.getDimension(R.dimen.margin_16).toInt()
        binding.setupExpandedControls.onApplyInsets { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = bottom + normalPadding)
        }
        whenResumed {
            binding.setupExpandedControlsNext.onClicked().collect {
                viewModel.onNextClicked()
            }
        }
    }

}