package com.kieronquinn.app.smartspacer.ui.screens.configuration.wifi.mac

import android.content.Context
import android.content.pm.PackageManager.ComponentInfoFlags
import android.content.pm.ProviderInfo
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.WiFiRequirement
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.test.BuildConfig
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WiFiRequirementConfigurationMACBottomSheetViewModelTests: BaseTest<WiFiRequirementConfigurationMACBottomSheetViewModel>() {

    companion object {
        private const val AUTHORITY_WIFI = "${BuildConfig.APPLICATION_ID}.requirement.wifi"
    }

    private var requirementDataMock = MutableStateFlow(WiFiRequirement.RequirementData(
        macAddress = randomString()
    ))

    private val navigationMock = mock<ConfigurationNavigation>()
    private val mockId = randomString()

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
        WiFiRequirementConfigurationMACBottomSheetViewModelImpl(
            dataRepositoryMock,
            navigationMock,
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
    fun testMac() = runTest {
        sut.mac.test {
            assertTrue(awaitItem() == null)
            sut.setupWithId(mockId)
            assertTrue(awaitItem() == requirementDataMock.value.macAddress)
        }
    }

    @Test
    fun testOnPositiveClicked() = runTest {
        val mockMac = "00:00:00:00:00:00"
        sut.setupWithId(mockId)
        assertFalse(sut.showError.value)
        sut.setMAC("invalid")
        sut.onPositiveClicked()
        coVerify(inverse = true) {
            navigationMock.navigateBack()
        }
        assertTrue(sut.showError.value)
        sut.setMAC(mockMac)
        assertFalse(sut.showError.value)
        sut.onPositiveClicked()
        assertTrue(requirementDataMock.value.macAddress == mockMac)
        coVerify {
            navigationMock.navigateBack()
        }
    }

    @Test
    fun testOnNegativeClicked() = runTest {
        sut.onNegativeClicked()
        coVerify {
            navigationMock.navigateBack()
        }
    }

    @Test
    fun testOnNeutralClicked() = runTest {
        val mockMac = "00:00:00:00:00:00"
        sut.setupWithId(mockId)
        sut.setMAC(mockMac)
        sut.onPositiveClicked()
        assertTrue(requirementDataMock.value.macAddress == mockMac)
        sut.onNeutralClicked()
        assertTrue(requirementDataMock.value.macAddress == null)
        coVerify {
            navigationMock.navigateBack()
        }
    }

}