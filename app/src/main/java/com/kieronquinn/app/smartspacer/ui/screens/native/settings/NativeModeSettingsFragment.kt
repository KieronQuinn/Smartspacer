package com.kieronquinn.app.smartspacer.ui.screens.native.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.HideBottomNavigation
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.native.settings.NativeModeSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class NativeModeSettingsFragment: BaseSettingsFragment(), BackAvailable, HideBottomNavigation {

    private val viewModel by viewModel<NativeModeSettingsViewModel>()
    private val args by navArgs<NativeModeSettingsFragmentArgs>()

    override val additionalPadding by lazy {
        requireContext().resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun shouldHideBottomNavigation(): Boolean {
        return !args.isFromSettings
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
        when(state){
            is State.Loading -> {
                binding.settingsBaseLoading.isVisible = true
                binding.settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.settingsBaseLoading.isVisible = false
                binding.settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), binding.settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> = listOfNotNull(
        GenericSettingsItem.Setting(
            getString(R.string.native_mode_settings_target_limit_title),
            getString(
                R.string.native_mode_settings_target_limit_content,
                getString(targetCountLimit.label)
            ),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_targets),
            onClick = { viewModel.onCountLimitClicked(args.isFromSettings) }
        ),
        GenericSettingsItem.SwitchSetting(
            hideIncompatibleTargets,
            getString(R.string.native_mode_settings_hide_incompatible_targets_title),
            getString(R.string.native_mode_settings_hide_incompatible_targets_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_native_mode_hide_incompatible),
            onChanged = viewModel::onHideIncompatibleChanged
        ),
        if(supportsSplitSmartspace) {
            GenericSettingsItem.SwitchSetting(
                useSplitSmartspace,
                getString(R.string.native_mode_settings_use_split_smartspace),
                getString(R.string.native_mode_settings_use_split_smartspace_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_complication_google_weather),
                onChanged = viewModel::onUseSplitSmartspaceChanged
            )
        }else null,
        GenericSettingsItem.SwitchSetting(
            immediateStart,
            getString(R.string.native_mode_immediate_start_title),
            getText(R.string.native_mode_immediate_start_content),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_reset),
            onChanged = viewModel::onImmediateStartChanged
        )
    )

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}