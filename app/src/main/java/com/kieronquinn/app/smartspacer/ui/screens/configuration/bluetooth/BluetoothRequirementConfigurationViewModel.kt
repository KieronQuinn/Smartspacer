package com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.BluetoothRequirement
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.BluetoothRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getNameOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BluetoothRequirementConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun setup(smartspacerId: String)
    abstract fun onResume()
    abstract fun onDeviceSelected(name: String)
    abstract fun onEnableClicked()
    abstract fun openAppInfo()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val isEnabled: Boolean,
            val hasPermission: Boolean,
            val hasBackgroundPermission: Boolean,
            val selected: String?,
            val hasSelectedItem: Boolean,
            val devices: List<Pair<String, BluetoothDevice>>
        ): State()
    }

}

class BluetoothRequirementConfigurationViewModelImpl(
    private val bluetoothRepository: BluetoothRepository,
    private val dataRepository: DataRepository,
    private val navigation: ConfigurationNavigation,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    scope: CoroutineScope? = null
): BluetoothRequirementConfigurationViewModel(scope) {

    private val smartspacerId = MutableStateFlow<String?>(null)

    private val complicationData = smartspacerId.filterNotNull().flatMapLatest {
        dataRepository.getRequirementDataFlow(it, RequirementData::class.java)
    }

    override val state = combine(
        bluetoothRepository.isEnabled,
        bluetoothRepository.hasPermission,
        bluetoothRepository.hasBackgroundPermission,
        bluetoothRepository.getBondedDevices(),
        complicationData
    ) { enabled, permissionGranted, backgroundPermissionGranted, devices, data ->
        val namedDevices = devices.mapNotNull {
            Pair(it.getNameOrNull() ?: return@mapNotNull null, it)
        }.sortedBy { it.first.lowercase() }
        val selected = data?.name
        val hasSelectedItem = selected != null && namedDevices.any {
            it.first == selected
        }
        State.Loaded(
            enabled,
            permissionGranted,
            backgroundPermissionGranted,
            selected,
            hasSelectedItem,
            namedDevices
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(smartspacerId: String) {
        vmScope.launch {
            this@BluetoothRequirementConfigurationViewModelImpl.smartspacerId.emit(smartspacerId)
        }
    }

    override fun onDeviceSelected(name: String) {
        val smartspacerId = smartspacerId.value ?: return
        dataRepository.updateRequirementData(
            smartspacerId,
            RequirementData::class.java,
            RequirementDataType.BLUETOOTH,
            ::onChanged
        ) {
            val data = it ?: RequirementData()
            data.copy(name = name)
        }
    }

    private fun onChanged(context: Context, smartspacerId: String) {
        SmartspacerRequirementProvider
            .notifyChange(context, BluetoothRequirement::class.java, smartspacerId)
    }

    override fun onResume() {
        bluetoothRepository.onPermissionChanged()
    }

    override fun onEnableClicked() {
        vmScope.launch {
            val result = shizukuServiceRepository.runWithService {
                it.enableBluetooth()
            }.unwrap() != null
            if(result) return@launch
            navigation.navigate(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    override fun openAppInfo() {
        vmScope.launch {
            navigation.navigate(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            })
        }
    }

}