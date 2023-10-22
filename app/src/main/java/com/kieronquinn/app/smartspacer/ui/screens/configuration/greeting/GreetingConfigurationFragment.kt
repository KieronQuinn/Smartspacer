package com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.greeting.GreetingConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class GreetingConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<GreetingConfigurationViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        val id = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID) ?: return
        viewModel.setupWithId(id)
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

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> = listOf(
        GenericSettingsItem.Setting(
            getString(R.string.target_greeting_configuration_name_title),
            if(name.isNotBlank()) {
                getString(R.string.target_greeting_configuration_name_content, name)
            }else{
                getString(R.string.target_greeting_configuration_name_content_empty)
            },
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_target_greeting
            ),
            onClick = viewModel::onNameClicked
        ),
        GenericSettingsItem.SwitchSetting(
            hideIfNoComplications,
            getString(R.string.target_greeting_configuration_hide_if_no_complications_title),
            getString(R.string.target_greeting_configuration_hide_if_no_complications_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_complications
            ),
            onChanged = viewModel::setHideIfNoComplications
        ),
        GenericSettingsItem.SwitchSetting(
            hideTitleOnAod,
            getString(R.string.target_greeting_configuration_hide_title_on_aod_title),
            getString(R.string.target_greeting_configuration_hide_title_on_aod_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_edit_show_on_lockscreen
            ),
            onChanged = viewModel::setHideTitleOnAod
        ),
        GenericSettingsItem.SwitchSetting(
            openExpandedOnClick,
            getString(R.string.target_greeting_configuration_open_expanded_title),
            getString(R.string.target_greeting_configuration_open_expanded_content),
            ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_targets
            ),
            onChanged = viewModel::setOpenExpandedOnClick
        )
    )

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}