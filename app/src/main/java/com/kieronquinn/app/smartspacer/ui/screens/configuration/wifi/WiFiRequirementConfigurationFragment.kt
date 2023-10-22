package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.smartspacer.BuildConfig.APPLICATION_ID
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.databinding.FragmentConfigurationRequirementWifiBinding
import com.kieronquinn.app.smartspacer.model.settings.BaseSettingsItem
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Card
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.Setting
import com.kieronquinn.app.smartspacer.model.settings.GenericSettingsItem.SwitchSetting
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.ui.base.BackAvailable
import com.kieronquinn.app.smartspacer.ui.base.BoundFragment
import com.kieronquinn.app.smartspacer.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.WiFiRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.smartspacer.utils.extensions.onClicked
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import org.koin.androidx.viewmodel.ext.android.viewModel

class WiFiRequirementConfigurationFragment: BoundFragment<FragmentConfigurationRequirementWifiBinding>(FragmentConfigurationRequirementWifiBinding::inflate), BackAvailable {

    private val viewModel by viewModel<WiFiRequirementConfigurationViewModel>()

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onResumed()
    }

    private val adapter by lazy {
        Adapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMonet()
        setupState()
        setupGrantPermissionButton()
        val id = requireActivity().intent.getStringExtra(EXTRA_SMARTSPACER_ID) ?: return
        viewModel.setupWithId(id)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumed()
    }

    private fun setupRecyclerView() = with(binding.requirementWifiConfigurationRecyclerView) {
        layoutManager = LinearLayoutManager(context)
        adapter = this@WiFiRequirementConfigurationFragment.adapter
        applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

    private fun setupMonet() {
        binding.requirementWifiConfigurationLoadingProgress.applyMonet()
        binding.requirementWifiConfigurationBackgroundLocationGrant.applyMonet()
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
                requirementWifiConfigurationLoading.isVisible = true
                requirementWifiConfigurationRecyclerView.isVisible = false
                requirementWifiConfigurationBackgroundLocationPermission.isVisible = false
            }
            is State.RequestPermissions -> {
                requirementWifiConfigurationLoading.isVisible = false
                requirementWifiConfigurationRecyclerView.isVisible = false
                requirementWifiConfigurationBackgroundLocationPermission.isVisible = true
                permissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE
                    )
                )
            }
            is State.RequestBackgroundLocation -> {
                requirementWifiConfigurationLoading.isVisible = false
                requirementWifiConfigurationRecyclerView.isVisible = false
                requirementWifiConfigurationBackgroundLocationPermission.isVisible = true
            }
            is State.Loaded -> {
                requireActivity().setResult(Activity.RESULT_OK)
                requirementWifiConfigurationLoading.isVisible = false
                requirementWifiConfigurationRecyclerView.isVisible = true
                requirementWifiConfigurationBackgroundLocationPermission.isVisible = false
                adapter.update(state.loadItems(), requirementWifiConfigurationRecyclerView)
            }
        }
    }

    private fun setupGrantPermissionButton() {
        whenResumed {
            binding.requirementWifiConfigurationBackgroundLocationGrant.onClicked().collect {
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", APPLICATION_ID, null)
                }.also {
                    startActivity(it)
                }
            }
        }
    }

    private fun State.Loaded.loadItems(): List<BaseSettingsItem> {
        return listOfNotNull(
            Setting(
                getString(R.string.requirement_wifi_configuration_pick_title),
                getString(R.string.requirement_wifi_configuration_pick_content),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_requirement_wifi_scan),
                onClick = viewModel::onShowNetworksClicked
            ),
            Setting(
                getString(R.string.requirement_wifi_configuration_ssid_title),
                data.ssid ?: getString(R.string.requirement_wifi_configuration_ssid_content_unset),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_requirement_wifi),
                onClick = viewModel::onSSIDClicked
            ),
            Setting(
                getString(R.string.requirement_wifi_configuration_mac_title),
                data.macAddress ?: getString(R.string.requirement_wifi_configuration_mac_content_unset),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_requirement_wifi_mac),
                onClick = viewModel::onMACClicked
            ),
            SwitchSetting(
                data.allowUnconnected,
                getString(R.string.requirement_wifi_configuration_in_range_title),
                getText(R.string.requirement_wifi_configuration_in_range_content),
                ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_requirement_wifi_allow_unconnected
                ),
                onChanged = viewModel::onAllowUnconnectedChanged
            ),
            if(!hasEnabledBackgroundScan){
                Card(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info),
                    getText(R.string.requirement_wifi_configuration_in_range_warning),
                    viewModel::onNetworkSettingsClicked
                )
            }else null
        )
    }

    inner class Adapter: BaseSettingsAdapter(
        binding.requirementWifiConfigurationRecyclerView, emptyList()
    )

}