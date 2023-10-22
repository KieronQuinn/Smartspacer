package com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.recenttask.RecentTaskRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class RecentTaskRequirementConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<RecentTaskRequirementConfigurationViewModel>()

    override val adapter by lazy {
        Adapter()
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        val id = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: return
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

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        val limit = limit?.toString()
            ?: getString(R.string.requirement_recent_apps_configuration_limit_content_unlimited)
        return listOf(
            Setting(
                getString(R.string.requirement_recent_apps_configuration_app_title),
                selectedAppName
                    ?: getString(R.string.requirement_recent_apps_configuration_app_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_recent_task_apps
                ),
                onClick = viewModel::onSelectedAppClicked
            ),
            Setting(
                getString(R.string.requirement_recent_apps_configuration_limit_title),
                getString(R.string.requirement_recent_apps_configuration_limit_content, limit),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_recent_task
                ),
                onClick = viewModel::onLimitClicked
            )
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}