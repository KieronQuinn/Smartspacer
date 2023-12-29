package com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Header
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.REQUIREMENT_BLUETOOTH
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsFragment
import com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth.BluetoothRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import org.koin.androidx.viewmodel.ext.android.viewModel

class BluetoothRequirementConfigurationFragment: BaseSettingsFragment(), BackAvailable {

    companion object {
        private const val EXTRA_IS_SETUP = "is_setup"

        fun getIntent(context: Context, isSetup: Boolean): Intent {
            return ConfigurationActivity.createIntent(context, REQUIREMENT_BLUETOOTH).apply {
                putExtra(EXTRA_IS_SETUP, isSetup)
            }
        }
    }

    private val viewModel by viewModel<BluetoothRequirementConfigurationViewModel>()

    private val requestPermission = registerForActivityResult(RequestMultiplePermissions()) {
        if(it.values.all { granted -> granted }) {
            viewModel.onResume()
        }else{
            viewModel.openAppInfo()
        }
    }

    private val isSetup by lazy {
        requireActivity().intent.getBooleanExtra(EXTRA_IS_SETUP, false)
    }

    override val adapter by lazy {
        Adapter()
    }

    override val additionalPadding by lazy {
        resources.getDimension(R.dimen.margin_8)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        viewModel.setup(requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID)!!)
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
                if(state.hasPermission && state.hasBackgroundPermission && state.hasSelectedItem
                    && isSetup) {
                    requireActivity().run {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        if(!hasPermission) {
            return listOf(
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_bluetooth),
                    getText(R.string.requirement_bluetooth_configuration_pick_permission),
                    ::onPermissionClicked
                )
            )
        }
        if(!hasBackgroundPermission) {
            return listOf(
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_target_calendar_settings_location),
                    getString(R.string.requirement_bluetooth_configuration_pick_background_permission),
                    ::onBackgroundPermissionClicked
                )
            )
        }
        if(!isEnabled) {
            return listOf(
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_bluetooth),
                    getString(R.string.requirement_bluetooth_configuration_pick_disabled),
                    viewModel::onEnableClicked
                )
            )
        }
        val options = devices.map {
            Setting(
                it.first,
                if(it.first == selected) {
                    getString(
                        R.string.requirement_bluetooth_configuration_pick_selected,
                        it.second.address
                    )
                }else{
                    it.second.address
                },
                null
            ) {
                viewModel.onDeviceSelected(it.first)
            }
        }.ifEmpty {
            listOf(Header(getString(R.string.requirement_bluetooth_configuration_pick_empty)))
        }
        return listOf(Card(
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
            getString(R.string.requirement_bluetooth_configuration_pick_info)
        )) + options
    }

    private fun onPermissionClicked() {
        requestPermission.launch(BluetoothRepository.BLUETOOTH_PERMISSIONS)
    }

    private fun onBackgroundPermissionClicked() {
        requestPermission.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    inner class Adapter: BaseSettingsAdapter(binding.settingsBaseRecyclerView, emptyList())

}