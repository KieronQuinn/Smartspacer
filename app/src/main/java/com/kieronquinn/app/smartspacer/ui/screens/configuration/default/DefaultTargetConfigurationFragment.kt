package com.kieronquinn.app.smartspacer.ui.screens.configuration.default

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
import com.kieronquinn.app.smartspacer.ui.screens.configuration.default.DefaultTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class DefaultTargetConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<DefaultTargetConfigurationViewModel>()

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        val id = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
        viewModel.setupWithId(id ?: return)
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
        when(state) {
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

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOf(
            GenericSettingsItem.Setting(
                getString(R.string.target_default_settings_more_title),
                getString(R.string.target_default_settings_more_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_target_at_a_glance),
                onClick = viewModel::onAtAGlanceClicked
            ),
            GenericSettingsItem.Header(getString(R.string.target_default_settings_hide_title)),
            GenericSettingsItem.Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                getText(R.string.target_default_settings_hide_infobox)
            ),
            *settings.map {
                GenericSettingsItem.SwitchSetting(
                    it.isEnabled,
                    getString(it.type.title),
                    "",
                    null
                ) { enabled ->
                    viewModel.onHiddenTargetChanged(it, enabled)
                }
            }.sortedBy { it.title.toString().lowercase() }.toTypedArray()
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}