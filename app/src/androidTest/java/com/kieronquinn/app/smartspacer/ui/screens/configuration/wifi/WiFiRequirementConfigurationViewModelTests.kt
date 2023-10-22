package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement.RequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.WiFiRequirementConfigurationViewModel.State
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WiFiRequirementConfigurationViewModelTests: BaseTest<WiFiRequirementConfigurationViewModel>() {

    companion object {
        private const val AUTHORITY_WIFI = "${BuildConfig.APPLICATION_ID}.requirement.wifi"
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
                RequirementDataType.WIFI,
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

    private val navigationMock = mock<ConfigurationNavigation>()
    private val wifiRepositoryMock = mock<WiFiRepository>()

    override val sut by lazy {
        WiFiRequirementConfigurationViewModelImpl(
            dataRepositoryMock,
            navigationMock,
            wifiRepositoryMock,
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
        every { wifiRepositoryMock.hasWiFiPermissions() } returns false
        every { wifiRepositoryMock.hasBackgroundLocationPermission() } returns false
        every { wifiRepositoryMock.hasEnabledBackgroundScanning() } returns false
        sut.state.test {
            assertTrue(awaitItem() is State.Loading)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() is State.RequestPermissions)
            every { wifiRepositoryMock.hasWiFiPermissions() } returns true
            sut.onResumed()
            assertTrue(awaitItem() is State.RequestBackgroundLocation)
            every { wifiRepositoryMock.hasBackgroundLocationPermission() } returns true
            sut.onResumed()
            assertTrue(awaitItem() is State.Loaded)
        }
    }

    @Test
    fun testOnAllowUnconnectedChanged() = runTest {
        sut.setupWithId(mockId)
        sut.onAllowUnconnectedChanged(false)
        assertFalse(requirementDataMock.value.allowUnconnected)
    }

    @Test
    fun testOnShowNetworksClicked() = runTest {
        sut.setupWithId(mockId)
        sut.onShowNetworksClicked()
        coVerify {
            navigationMock.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationPickerFragment(mockId)
            )
        }
    }

    @Test
    fun testOnSSIDClicked() = runTest {
        sut.setupWithId(mockId)
        sut.onSSIDClicked()
        coVerify {
            navigationMock.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationSSIDBottomSheetFragment(mockId)
            )
        }
    }

    @Test
    fun testOnMACClicked() = runTest {
        sut.setupWithId(mockId)
        sut.onMACClicked()
        coVerify {
            navigationMock.navigate(
                WiFiRequirementConfigurationFragmentDirections
                    .actionWiFiRequirementConfigurationFragmentToWiFiRequirementConfigurationMACBottomSheetFragment(mockId)
            )
        }
    }

    @Test
    fun testOnNetworkSettingsClicked() = runTest {
        sut.onNetworkSettingsClicked()
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

}