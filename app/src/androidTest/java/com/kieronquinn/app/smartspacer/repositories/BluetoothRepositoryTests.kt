package com.kieronquinn.app.smartspacer.repositories

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.utils.extensions.isConnected
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BluetoothRepositoryTests: BaseTest<BluetoothRepository>() {

    companion object {
        private const val AUTHORITY_BLUETOOTH = "${BuildConfig.APPLICATION_ID}.requirement.bluetooth"

        private fun mockBluetoothDevice(isConnected: Boolean): BluetoothDevice {
            return mockk {
                every { isConnected() } returns isConnected
                every { name } returns randomString()
            }
        }
    }

    private val bluetoothAdapterMock = mockk<BluetoothAdapter> {
        every { isEnabled } answers { isEnabledMock }
        every { bondedDevices } answers { bondedDevicesMock }
    }

    private val bluetoothManagerMock = mockk<BluetoothManager> {
        every { adapter } returns bluetoothAdapterMock
    }

    private var bondedDevicesMock = emptySet<BluetoothDevice>()
    private var isEnabledMock = true
    private var permissionResponseMock = PackageManager.PERMISSION_GRANTED

    override val sut by lazy {
        BluetoothRepositoryImpl(contextMock)
    }

    override fun Context.context() {
        every { getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManagerMock
        every {
            checkCallingOrSelfPermission(any())
        } answers {
            permissionResponseMock
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) } returns true
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_BLUETOOTH
            }
        }
    }

    @Test
    fun testConnectedDevices() = runTest {
        sut.connectedDevices.test {
            assertTrue(awaitItem().isEmpty())
            bondedDevicesMock = setOf(
                mockBluetoothDevice(true),
                mockBluetoothDevice(false)
            )
            contextMock.sendBroadcast(Intent(BluetoothDevice.ACTION_ACL_CONNECTED))
            assertTrue(awaitItem().size == 1)
        }
    }

    @Test
    fun testHasPermission() = runTest {
        sut.hasPermission.test {
            assertTrue(awaitItem())
            permissionResponseMock = PackageManager.PERMISSION_DENIED
            sut.onPermissionChanged()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testHasBackgroundPermission() = runTest {
        sut.hasBackgroundPermission.test {
            assertTrue(awaitItem())
            permissionResponseMock = PackageManager.PERMISSION_DENIED
            sut.onPermissionChanged()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testIsEnabled() = runTest {
        sut.isEnabled.test {
            assertTrue(awaitItem())
            isEnabledMock = false
            contextMock.sendBroadcast(Intent(BluetoothAdapter.ACTION_STATE_CHANGED))
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testGetBondedDevices() = runTest {
        sut.getBondedDevices().test {
            assertTrue(awaitItem().isEmpty())
            bondedDevicesMock = setOf(
                mockBluetoothDevice(true),
                mockBluetoothDevice(false)
            )
            contextMock.sendBroadcast(Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            assertTrue(awaitItem().size == 2)
        }
    }

}