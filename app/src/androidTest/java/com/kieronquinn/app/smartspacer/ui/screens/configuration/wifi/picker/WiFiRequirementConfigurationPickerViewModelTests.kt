package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository.WiFiNetwork
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.picker.WiFiRequirementConfigurationPickerViewModel.State
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WiFiRequirementConfigurationPickerViewModelTests: BaseTest<WiFiRequirementConfigurationPickerViewModel>() {

    companion object {
        private const val AUTHORITY_WIFI = "${BuildConfig.APPLICATION_ID}.requirement.wifi"

        private fun createMockWiFiNetwork(): WiFiNetwork {
            return WiFiNetwork(randomString(), randomString())
        }

        private fun createMockWiFiNetworks(): List<WiFiNetwork> {
            return listOf(
                createMockWiFiNetwork(),
                createMockWiFiNetwork(),
                createMockWiFiNetwork()
            )
        }
    }

    private var requirementDataMock = MutableStateFlow(WiFiRequirement.RequirementData())
    private val navigationMock = mock<ConfigurationNavigation>()
    private val enhancedModeEnabledMock = mockSmartspacerSetting(false)
    private val savedNetworksMock = createMockWiFiNetworks()
    private val availableNetworksMock = createMockWiFiNetworks()
    private val connectedNetworkMock = createMockWiFiNetwork()
    private val mockId = randomString()

    private val wifiRepositoryMock = mock<WiFiRepository> {
        coEvery { getSavedWiFiNetworks() } returns savedNetworksMock
        every { availableNetworks } returns MutableStateFlow(availableNetworksMock)
        every { connectedNetwork } returns MutableStateFlow(connectedNetworkMock)
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { enhancedMode } returns enhancedModeEnabledMock
    }

    private val dataRepositoryMock = mock<DataRepository> {
        every {
            getRequirementDataFlow(any(), WiFiRequirement.RequirementData::class.java)
        } answers {
            requirementDataMock
        }
        every {
            updateRequirementData(
                any(),
                WiFiRequirement.RequirementData::class.java,
                RequirementDataType.WIFI,
                any(),
                any()
            )
        } coAnswers {
            val onComplete = arg<((context: Context, smartspacerId: String) -> Unit)?>(3)
            val update = arg<(Any?) -> Any>(4)
            val newData = update.invoke(requirementDataMock.value)
            requirementDataMock.emit(newData as WiFiRequirement.RequirementData)
            onComplete?.invoke(contextMock, mockId)
        }
    }

    override val sut by lazy {
        WiFiRequirementConfigurationPickerViewModelImpl(
            wifiRepositoryMock,
            dataRepositoryMock,
            navigationMock,
            settingsRepositoryMock,
            scope
        )
    }

    override fun setup() {
        super.setup()
        every { packageManagerMock.getProviderInfo(any(), any<ComponentInfoFlags>()) } answers {
            ProviderInfo().apply {
                authority = AUTHORITY_WIFI
            }
        }
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.connectedNetwork == connectedNetworkMock)
            assertTrue(item.availableNetworks == availableNetworksMock.sort())
            assertTrue(item.savedNetworks.isEmpty())
            enhancedModeEnabledMock.emit(true)
            sut.refresh()
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.connectedNetwork == connectedNetworkMock)
            assertTrue(updatedItem.availableNetworks == availableNetworksMock.sort())
            assertTrue(updatedItem.savedNetworks == savedNetworksMock.sort())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testOnNetworkClicked() = runTest {
        sut.setupWithId(mockId)
        sut.onNetworkClicked(connectedNetworkMock)
        assertTrue(requirementDataMock.value.macAddress == connectedNetworkMock.mac)
        assertTrue(requirementDataMock.value.ssid == connectedNetworkMock.ssid)
        coVerify {
            navigationMock.navigateBack()
        }
    }

    private fun List<WiFiNetwork>.sort(): List<WiFiNetwork> {
        return sortedBy { network -> network.ssid?.lowercase() }
    }

}