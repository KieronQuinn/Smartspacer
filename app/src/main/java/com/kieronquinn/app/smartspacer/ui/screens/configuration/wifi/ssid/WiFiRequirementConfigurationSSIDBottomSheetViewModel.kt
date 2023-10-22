package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.ssid

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

abstract class WiFiRequirementConfigurationSSIDBottomSheetViewModel(
    scope: CoroutineScope?
): BaseViewModel(scope) {

    abstract val ssid: StateFlow<String?>

    abstract fun setupWithId(smartspacerId: String)
    abstract fun setSSID(ssid: String)
    abstract fun onPositiveClicked()
    abstract fun onNegativeClicked()
    abstract fun onNeutralClicked()

}

class WiFiRequirementConfigurationSSIDBottomSheetViewModelImpl(
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    scope: CoroutineScope? = null
): WiFiRequirementConfigurationSSIDBottomSheetViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val requirementData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java).map { req ->
            req ?: RequirementData()
        }
    }

    private val _ssid = MutableStateFlow<String?>(null)

    override val ssid = combine(_ssid, requirementData) { ssid, req ->
        ssid ?: req.ssid ?: ""
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override fun setupWithId(smartspacerId: String) {
        vmScope.launch {
            this@WiFiRequirementConfigurationSSIDBottomSheetViewModelImpl
                .smartspacerId.emit(smartspacerId)
        }
    }

    override fun setSSID(ssid: String) {
        vmScope.launch {
            _ssid.emit(ssid)
        }
    }

    override fun onPositiveClicked() {
        vmScope.launch {
            val ssid = ssid.value?.takeIf { it.isNotBlank() }
            commitSSID(ssid)
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
            commitSSID(null)
            navigation.navigateBack()
        }
    }

    private fun commitSSID(ssid: String?) {
        val id = smartspacerId.value ?: return
        dataRepository.updateRequirementData(
            id,
            RequirementData::class.java,
            RequirementDataType.WIFI,
            ::onSettingsUpdated
        ) {
            it?.copy(ssid = ssid) ?: RequirementData(ssid = ssid)
        }
    }

    private fun onSettingsUpdated(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider.notifyChange(
            context, WiFiRequirement::class.java, smartspacerId
        )
    }

}