package com.kieronquinn.app.smartspacer.ui.screens.repository.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.PluginRepositorySettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class PluginRepositorySettingsFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<PluginRepositorySettingsViewModel>()

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
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

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val items = if(enabled){
            arrayOf(
                GenericSettingsItem.SwitchSetting(
                    updateCheckEnabled,
                    getString(R.string.plugin_settings_update_check_enabled_title),
                    getString(R.string.plugin_settings_update_check_enabled_content),
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_updates),
                    onChanged = viewModel::onUpdateCheckEnabledChanged
                ),
                GenericSettingsItem.Setting(
                    getString(R.string.plugin_settings_url_title),
                    getText(R.string.plugin_settings_url_content),
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_settings_plugin_repository_url
                    ),
                    onClick = viewModel::onUrlClicked
                )
            )
        } else emptyArray()
        return listOf(
            GenericSettingsItem.Switch(
                enabled,
                getString(R.string.plugin_settings_enabled),
                viewModel::onEnabledChanged
            ),
            *items,
            GenericSettingsItem.Footer(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                getString(R.string.plugin_settings_footer)
            )
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}