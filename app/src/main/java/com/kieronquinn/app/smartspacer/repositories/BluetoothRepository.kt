package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.BluetoothRequirement
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository.Companion.BLUETOOTH_PERMISSIONS
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.utils.extensions.broadcastReceiverAsFlow
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.isConnected
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface BluetoothRepository {

    companion object {
        val BLUETOOTH_PERMISSIONS = arrayOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.BLUETOOTH_CONNECT
            } else {
                Manifest.permission.BLUETOOTH
            },
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val isCompatible: Boolean
    val connectedDevices: StateFlow<List<BluetoothDevice>>
    val hasPermission: StateFlow<Boolean>
    val hasBackgroundPermission: StateFlow<Boolean>
    val isEnabled: StateFlow<Boolean>

    fun onPermissionChanged()
    fun getBondedDevices(): Flow<Set<BluetoothDevice>>

}

class BluetoothRepositoryImpl(private val context: Context): BluetoothRepository {

    companion object {
        private val BLUETOOTH_UPDATE_FILTER = arrayOf(
            BluetoothAdapter.ACTION_STATE_CHANGED,
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
        ).let {
            IntentFilter().apply {
                it.forEach { action -> addAction(action) }
            }
        }
    }

    private val scope = MainScope()

    private val hasBluetooth =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val permissionChangedBus = MutableStateFlow(System.currentTimeMillis())

    override val isCompatible: Boolean
        get() = hasBluetooth

    override val hasPermission = permissionChangedBus.mapLatest {
        doesHavePermission()
    }.stateIn(scope, SharingStarted.Eagerly, doesHavePermission())

    override val hasBackgroundPermission = permissionChangedBus.mapLatest {
        doesHaveBackgroundLocation()
    }.stateIn(scope, SharingStarted.Eagerly, doesHaveBackgroundLocation())

    private val devicesChanged = context.broadcastReceiverAsFlow(
        BLUETOOTH_UPDATE_FILTER
    ).map {
        System.currentTimeMillis()
    }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    @SuppressLint("MissingPermission")
    override val connectedDevices = combine(
        devicesChanged,
        hasPermission,
        hasBackgroundPermission
    ) { _, _, _ ->
        try {
            bluetoothManager.adapter.bondedDevices.filter { device ->
                device.isConnected()
            }
        }catch (e: Exception) {
            emptyList()
        }
    }.onEach {
        SmartspacerRequirementProvider.notifyChange(context, BluetoothRequirement::class.java)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val isEnabled by lazy {
        context.broadcastReceiverAsFlow(
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ).map {
            isEnabled()
        }.stateIn(scope, SharingStarted.Eagerly, isEnabled())
    }

    override fun onPermissionChanged() {
        scope.launch {
            permissionChangedBus.emit(System.currentTimeMillis())
        }
    }

    @SuppressLint("MissingPermission")
    override fun getBondedDevices(): Flow<Set<BluetoothDevice>> {
        val hasAllPermissions = combine(hasPermission, hasBackgroundPermission) { a, b ->
            a && b
        }
        val bondedDevicesChanged = hasAllPermissions.flatMapLatest {
            if(!it) return@flatMapLatest flowOf(null)
            context.broadcastReceiverAsFlow(
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                }
            ).map {
                System.currentTimeMillis()
            }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())
        }
        return bondedDevicesChanged.mapLatest {
            if(it == null) return@mapLatest emptySet()
            try {
                bluetoothManager.adapter.bondedDevices ?: emptySet()
            }catch (e: Exception) {
                emptySet()
            }
        }
    }

    private fun doesHavePermission(): Boolean {
        return hasBluetooth && context.hasPermission(*BLUETOOTH_PERMISSIONS)
    }

    private fun doesHaveBackgroundLocation(): Boolean {
        return context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun isEnabled(): Boolean {
        return bluetoothManager.adapter.isEnabled
    }

}