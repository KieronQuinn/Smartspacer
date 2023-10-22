package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WiFiRequirementConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onResumed()
    abstract fun setupWithId(smartspacerId: String)
    abstract fun onAllowUnconnectedChanged(enabled: Boolean)
    abstract fun onShowNetworksClicked()
    abstract fun onSSIDClicked()
    abstract fun onMACClicked()
    abstract fun onNetworkSettingsClicked()

    sealed class State {
        object Loading: State()
        object RequestPermissions: State()
        object RequestBackgroundLocation: State()
        data class Loaded(val data: RequirementData, val hasEnabledBackgroundScan: Boolean): State()
    }

}

class WiFiRequirementConfigurationViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    wifiRepository: WiFiRepository,
    scope: CoroutineScope? = null
): WiFiRequirementConfigurationViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val requirementData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java)
    }

    private val hasWifiPermissions = resumeBus.mapLatest {
        wifiRepository.hasWiFiPermissions()
    }

    private val hasBackgroundLocationPermission = resumeBus.mapLatest {
        wifiRepository.hasBackgroundLocationPermission()
    }

    private val hasEnabledBackgroundScanning = resumeBus.mapLatest {
        wifiRepository.hasEnabledBackgroundScanning()
    }

    override val state = combine(
        requirementData,
        hasWifiPermissions,
        hasBackgroundLocationPermission,
        hasEnabledBackgroundScanning
    ) { data, wifi, location, scan ->
        when {
            !wifi -> State.RequestPermissions
            !location -> State.RequestBackgroundLocation
            else -> State.Loaded(data ?: RequirementData(), scan)
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onResumed() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            this@WiFiRequirementConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onAllowUnconnectedChanged(enabled: Boolean) {
        val id = smartspacerId.value ?: return
        dataRepository.updateRequirementData(
            id,
            RequirementData::class.java,
            RequirementDataType.WIFI,
            ::onSettingsUpdated
        ) {
            RequirementData(it?.ssid, it?.macAddress, enabled)
        }
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider.notifyChange(
            context, WiFiRequirement::class.java, smartspacerId
        )
    }

    override fun onSSIDClicked() {
        vmScope.launch {
            val id = smartspacerId.value ?: return@launch
            navigation.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationSSIDBottomSheetFragment(id)
            )
        }
    }

    override fun onMACClicked() {
        vmScope.launch {
            val id = smartspacerId.value ?: return@launch
            navigation.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationMACBottomSheetFragment(id)
            )
        }
    }

    override fun onShowNetworksClicked() {
        vmScope.launch {
            val id = smartspacerId.value ?: return@launch
            navigation.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationPickerFragment(id)
            )
        }
    }

    override fun onNetworkSettingsClicked() {
        vmScope.launch {
            navigation.navigate(Intent(Settings.ACTION_WIFI_IP_SETTINGS))
        }
    }

}