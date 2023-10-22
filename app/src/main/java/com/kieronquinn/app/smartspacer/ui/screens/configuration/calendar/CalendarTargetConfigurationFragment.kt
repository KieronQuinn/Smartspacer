package com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.targets.CalendarTarget.TargetData.PreEventTime
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.calendar.CalendarTargetConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class CalendarTargetConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    private val viewModel by viewModel<CalendarTargetConfigurationViewModel>()

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPermissionResult(requireContext(), it)
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        if(!requireContext().hasPermission(Manifest.permission.READ_CALENDAR)){
            viewModel.requestPermission(permissionRequest)
        }
        val id = requireActivity().intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
        viewModel.setupWithId(id ?: return)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
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
                if(state.targetData.calendars.isNotEmpty()) {
                    requireActivity().setResult(Activity.RESULT_OK)
                }
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOf<BaseSettingsItem>(
            GenericSettingsItem.Header(getString(R.string.target_calendar_settings_calendars)),
            *calendars.map {
                GenericSettingsItem.SwitchSetting(
                    targetData.calendars.contains(it.id),
                    it.name,
                    it.account,
                    null,
                ) { enabled ->
                    viewModel.onCalendarChanged(it.id, enabled)
                }
            }.toTypedArray(),
            GenericSettingsItem.Header(getString(R.string.target_calendar_settings_settings)),
            GenericSettingsItem.SwitchSetting(
                targetData.showAllDay,
                getString(R.string.target_calendar_settings_show_all_day_title),
                getString(R.string.target_calendar_settings_show_all_day_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_show_all_day
                ),
                onChanged = viewModel::onShowAllDayChanged
            ),
            GenericSettingsItem.SwitchSetting(
                targetData.showLocation,
                getString(R.string.target_calendar_settings_show_location_title),
                getString(R.string.target_calendar_settings_show_location_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_location
                ),
                onChanged = viewModel::onShowLocationChanged
            ),
            GenericSettingsItem.SwitchSetting(
                targetData.showUnconfirmed,
                getString(R.string.target_calendar_settings_show_unconfirmed_title),
                getString(R.string.target_calendar_settings_show_unconfirmed_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_show_unconfirmed
                ),
                onChanged = viewModel::onShowUnconfirmedChanged
            ),
            GenericSettingsItem.SwitchSetting(
                targetData.useAlternativeEventIds,
                getString(R.string.target_calendar_settings_use_alternative_event_id_title),
                getString(R.string.target_calendar_settings_use_alternative_event_id_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_use_alternative_ids
                ),
                onChanged = viewModel::onUseAlternativeIdsChanged
            ),
            GenericSettingsItem.Dropdown(
                getString(R.string.target_calendar_settings_pre_event_time_title),
                getString(targetData.preEventTime.label),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_pre_event_time
                ),
                targetData.preEventTime,
                viewModel::onPreEventTimeChanged,
                PreEventTime.values().toList()
            ) {
                it.label
            },
            GenericSettingsItem.Setting(
                getString(R.string.target_calendar_settings_clear_dismissed_events_title),
                getString(R.string.target_calendar_settings_clear_dismissed_events_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_target_calendar_settings_clear_dismissed_events
                ),
                onClick = viewModel::onClearDismissEventsClicked
            )
        )
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}