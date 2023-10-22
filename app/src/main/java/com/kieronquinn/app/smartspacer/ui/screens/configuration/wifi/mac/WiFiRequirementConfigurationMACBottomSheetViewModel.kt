package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.mac

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WiFiRequirementConfigurationMACBottomSheetViewModel(
    scope: CoroutineScope?
): BaseViewModel(scope) {

    abstract val mac: StateFlow<String?>
    abstract val showError: StateFlow<Boolean>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun setMAC(mac: String)
    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()
    abstract fun onNeutralClicked()

}

class WiFiRequirementConfigurationMACBottomSheetViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): WiFiRequirementConfigurationMACBottomSheetViewModel(scope) {

    companion object {
        private val REGEX_MAC = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\$".toRegex()
    }

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val requirementData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java).map { req ->
            req ?: RequirementData()
        }
    }

    private val _mac = MutableStateFlow<String?>(null)

    override val showError = MutableStateFlow(false)

    override val mac = combine(_mac, requirementData) { mac, req ->
        mac ?: req.macAddress ?: ""
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            this@WiFiRequirementConfigurationMACBottomSheetViewModelImpl
                .smartspacerId.emit(smartspacerId)
        }
    }

    override fun setMAC(mac: String) {
        vmScope.launch {
            _mac.emit(mac)
            showError.emit(false)
        }
    }

    override fun onPositiveClicked() {
        vmScope.launch {
            val mac = mac.value?.takeIf { it.isNotBlank() }
            if(!commitMAC(mac)) return@launch
            navigation.navigateBack()
        }
    }

    override fun onNegativeClicked() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onNeutralClicked() {
        vmScope.launch {
            if(!commitMAC(null)) return@launch
            navigation.navigateBack()
        }
    }

    private suspend fun commitMAC(mac: String?): Boolean {
        val id = smartspacerId.value ?: return false
        if(mac != null && !REGEX_MAC.matches(mac)){
            showError.emit(true)
            return false
        }
        dataRepository.updateRequirementData(
            id,
            RequirementData::class.java,
            RequirementDataType.WIFI,
            ::onSettingsUpdated
        ) {
            it?.copy(macAddress = mac) ?: RequirementData(macAddress = mac)
        }
        return true
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider.notifyChange(
            context, WiFiRequirement::class.java, smartspacerId
        )
    }

}