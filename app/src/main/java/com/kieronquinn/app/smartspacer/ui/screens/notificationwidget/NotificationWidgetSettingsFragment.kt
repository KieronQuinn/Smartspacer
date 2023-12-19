package com.kieronquinn.app.smartspacer.ui.screens.notificationwidget

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Switch
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.notificationwidget.NotificationWidgetSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationWidgetSettingsFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<NotificationWidgetSettingsViewModel>()

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        whenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) = with(binding) {
        when(state) {
            is State.Loading -> {
                settingsBaseLoading.isVisible = true
                settingsBaseRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                settingsBaseLoading.isVisible = false
                settingsBaseRecyclerView.isVisible = true
                adapter.update(state.loadItems(), settingsBaseRecyclerView)
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOfNotNull(
            Switch(
                enabled,
                getString(R.string.notification_widget_toggle),
                viewModel::onEnabledChanged
            ),
            GenericSettingsItem.Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                getText(R.string.notification_widget_info)
            ).takeIf { notificationsEnabled },
            GenericSettingsItem.Card(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning),
                getText(R.string.notification_widget_notifications_disabled),
                viewModel::onNotificationsClicked
            ).takeUnless { notificationsEnabled },
            GenericSettingsItem.Dropdown(
                getString(R.string.notification_widget_tint_title),
                getString(R.string.notification_widget_tint_content, getString(tintColour.labelAlt)),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_expanded_tint_colour),
                tintColour,
                viewModel::onTintColourChanged,
                TintColour.entries
            ) {
                it.labelAlt
            }.takeIf { enabled }
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}