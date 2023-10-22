package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository.WiFiNetwork
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class WiFiRequirementConfigurationPickerViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String

    abstract fun setupWithId(smartspacerId: String)
    abstract fun refresh()
    abstract fun onNetworkClicked(network: WiFiNetwork)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val smartspacerId: String,
            val isScanning: Boolean,
            val isSearching: Boolean,
            val connectedNetwork: WiFiNetwork?,
            val availableNetworks: List<WiFiNetwork>,
            val savedNetworks: List<WiFiNetwork>
        ): State()
    }

}

class WiFiRequirementConfigurationPickerViewModelImpl(
    private val wiFiRepository: WiFiRepository,
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): WiFiRequirementConfigurationPickerViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)
    private val isScanning = MutableStateFlow(true)
    private val refreshBus = MutableStateFlow(System.currentTimeMillis())

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val showSearchClear = searchTerm.map { it.isNotBlank() }
        .stateIn(vmScope, SharingStarted.Eagerly, false)

    private val savedNetworks = combine(
        refreshBus,
        settingsRepository.enhancedMode.asFlow()
    ) { _, enhanced ->
        if(enhanced){
            wiFiRepository.getSavedWiFiNetworks()
        }else emptyList()
    }.map {
        it.distinctBy { network -> network.ssid }
            .sortedBy { network -> network.ssid?.lowercase() }
    }

    private val availableNetworks = wiFiRepository.availableNetworks.onEach {
        isScanning.emit(false)
    }.map {
        it.sortedBy { network -> network.ssid?.lowercase() }
    }

    private val networks = combine(
        wiFiRepository.connectedNetwork,
        availableNetworks,
        savedNetworks
    ) { connected, available, saved ->
        Triple(connected, available, saved)
    }

    override val state = combine(
        smartspacerId.filterNotNull(),
        isScanning,
        searchTerm,
        networks,
    ) { id, scanning, search, networks ->
        val connected = networks.first?.filterMatches(search)
        val available = networks.second.mapNotNull { it.filterMatches(search) }
        val saved = networks.third.mapNotNull { it.filterMatches(search) }
        val availableWithoutCurrent = available.filterNot {
            connected != null && connected.ssid == it.ssid && connected.mac == it.mac
        }
        State.Loaded(id, scanning, search.isNotBlank(), connected, availableWithoutCurrent, saved)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private fun WiFiNetwork.filterMatches(searchTerm: String): WiFiNetwork? {
        if(searchTerm.isBlank()) return this
        return takeIf {
            it.ssid?.contains(searchTerm, true) == true ||
                    it.mac?.contains(searchTerm, true) == true
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun refresh() {
        vmScope.launch {
            isScanning.emit(true)
            wiFiRepository.refresh()
            refreshBus.emit(System.currentTimeMillis())
        }
    }

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            this@WiFiRequirementConfigurationPickerViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onNetworkClicked(network: WiFiNetwork) {
        val id = (state.value as? State.Loaded)?.smartspacerId ?: return
        vmScope.launch {
            dataRepository.updateRequirementData(
                id,
                RequirementData::class.java,
                RequirementDataType.WIFI,
                ::onSettingsUpdated
            ) {
                it?.copy(ssid = network.ssid, macAddress = network.mac)
                    ?: RequirementData(ssid = network.ssid, macAddress = network.mac)
            }
            navigation.navigateBack()
        }
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider.notifyChange(
            context, WiFiRequirement::class.java, smartspacerId
        )
    }

}