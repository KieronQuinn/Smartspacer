package com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Footer
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Switch
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel.SettingsOemSmartspaceSettingsItem.App
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsOemSmartspaceFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<SettingsOemSmartspaceViewModel>()

    override val adapter by lazy {
        SettingsOemSmartspaceAdapter(binding.settingsBaseRecyclerView, emptyList())
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
        val header = if(compatible) {
            Switch(
                enabled,
                getString(R.string.oem_smartspace_switch),
                viewModel::onEnabledChanged
            )
        } else {
            Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning),
                getString(R.string.oem_smartspace_incompatible),
                viewModel::onIncompatibleClicked
            )
        }
        val apps = when {
            !compatible || !enabled -> emptyArray()
            apps.isNotEmpty() -> {
                val appsHeader = Header(getString(R.string.oem_smartspace_apps))
                val items = apps.map { app ->
                    val warning = if(app.mayRequireAdditionalSetup) {
                        getString(R.string.oem_smartspace_warning)
                    } else null
                    App(app, warning) { enabled ->
                        viewModel.onAppChanged(app.packageName, enabled)
                    }
                }.toTypedArray()
                listOf(appsHeader, *items).toTypedArray()
            }
            else -> arrayOf(
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                    getString(R.string.oem_smartspace_no_apps)
                )
            )
        }
        val hideIncompatible = if(compatible && enabled && apps.isNotEmpty()){
            arrayOf(
                Header(getString(R.string.oem_smartspace_settings)),
                SwitchSetting(
                    hideIncompatible,
                    getString(R.string.oem_smartspace_hide_incompatible_title),
                    getString(R.string.oem_smartspace_hide_incompatible_content),
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_native_mode_hide_incompatible
                    ),
                    onChanged = viewModel::onHideIncompatibleChanged
                )
            )
        } else emptyArray()
        val footer = Footer(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
            getText(R.string.oem_smartspace_info),
            getString(R.string.oem_smartspace_more_info),
            viewModel::onReadMoreClicked
        )
        return listOfNotNull(header, *apps, *hideIncompatible, footer)
    }

}