package com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.BluetoothRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth.BluetoothRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.extensions.getNameOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.isConnected
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BluetoothRequirementConfigurationViewModelTests: BaseTest<BluetoothRequirementConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_BLUETOOTH = "${BuildConfig.APPLICATION_ID}.requirement.bluetooth"

        private fun mockBluetoothDevice(isConnected: Boolean): BluetoothDevice {
            return mockk {
                every { isConnected() } returns isConnected
                every { name } returns randomString()
            }
        }
    }

    override val sut by lazy {
        BluetoothRequirementConfigurationViewModelImpl(
            bluetoothRepositoryMock,
            dataRepositoryMock,
            navigationMock,
            shizukuServiceRepositoryMock,
            scope
        )
    }

    private val isEnabledMock = MutableStateFlow(false)
    private val hasPermissionMock = MutableStateFlow(false)
    private val hasBackgroundPermissionMock = MutableStateFlow(false)
    private val bondedDevicesMock = MutableStateFlow(emptySet<BluetoothDevice>())

    private val navigationMock = mockk<ConfigurationNavigation>()
    private val shizukuServiceRepositoryMock = mockShizukuRepository {  }

    private val bluetoothRepositoryMock = mockk<BluetoothRepository> {
        every { isEnabled } returns isEnabledMock
        every { hasPermission } returns hasPermissionMock
        every { hasBackgroundPermission } returns hasBackgroundPermissionMock
        every { getBondedDevices() } returns bondedDevicesMock
    }

    private var requirementDataMock = MutableStateFlow(RequirementData())
    private val mockId = randomString()

    private val dataRepositoryMock = mock<DataRepository> {
        every {
            getRequirementDataFlow(any(), RequirementData::class.java)
        } answers {
            requirementDataMock
        }
        every {
            updateRequirementData(
                any(),
                RequirementData::class.java,
                RequirementDataType.BLUETOOTH,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(requirementDataMock.value)
            requirementDataMock.emit(newData as RequirementData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_BLUETOOTH
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setup(randomString())
            var item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertFalse(item.isEnabled)
            assertFalse(item.hasPermission)
            assertFalse(item.hasBackgroundPermission)
            assertTrue(item.devices.isEmpty())
            isEnabledMock.emit(true)
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.isEnabled)
            assertFalse(item.hasPermission)
            assertFalse(item.hasBackgroundPermission)
            assertTrue(item.devices.isEmpty())
            hasPermissionMock.emit(true)
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.isEnabled)
            assertTrue(item.hasPermission)
            assertFalse(item.hasBackgroundPermission)
            assertTrue(item.devices.isEmpty())
            hasBackgroundPermissionMock.emit(true)
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.isEnabled)
            assertTrue(item.hasPermission)
            assertTrue(item.hasBackgroundPermission)
            assertTrue(item.devices.isEmpty())
            val devices =
                setOf(mockBluetoothDevice(true), mockBluetoothDevice(false))
            val namedDevices = devices.mapNotNull {
                Pair(it.getNameOrNull() ?: return@mapNotNull null, it)
            }.sortedBy { it.first.lowercase() }
            bondedDevicesMock.emit(devices)
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.isEnabled)
            assertTrue(item.hasPermission)
            assertTrue(item.hasBackgroundPermission)
            assertTrue(item.devices == namedDevices)
        }
    }

    @Test
    fun testOnDeviceSelected() = runTest {
        isEnabledMock.emit(true)
        hasPermissionMock.emit(true)
        hasBackgroundPermissionMock.emit(true)
        val devices =
            setOf(mockBluetoothDevice(true), mockBluetoothDevice(false))
        bondedDevicesMock.emit(devices)
        sut.setup(randomString())
        sut.state.test {
            var item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.selected == null)
            assertFalse(item.hasSelectedItem)
            val itemToSelect = devices.first()
            sut.onDeviceSelected(itemToSelect.name)
            item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.selected == itemToSelect.name)
            assertTrue(item.hasSelectedItem)
        }
    }

}