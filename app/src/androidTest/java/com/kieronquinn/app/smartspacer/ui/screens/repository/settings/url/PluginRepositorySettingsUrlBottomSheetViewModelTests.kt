package com.kieronquinn.app.smartspacer.ui.screens.repository.settings.url

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginRepositorySettingsUrlBottomSheetViewModelTests: BaseTest<PluginRepositorySettingsUrlBottomSheetViewModel>() {

    private val pluginRepositoryUrlMock = mockSmartspacerSetting("https://example.com")
    private val navigationMock = mock<ContainerNavigation>()

    private val settingsMock = mock<SmartspacerSettingsRepository> {
        every { pluginRepositoryUrl } returns pluginRepositoryUrlMock
    }

    override val sut by lazy {
        PluginRepositorySettingsUrlBottomSheetViewModelImpl(
            settingsMock,
            navigationMock,
            scope
        )
    }

    @Test
    fun testSetName() = runTest {
        assertTrue(sut.url == "https://example.com")
        sut.setUrl("https://google.com")
        assertTrue(sut.url == "https://google.com")
    }

    @Test
    fun testOnPositiveClicked() = runTest {
        sut.setUrl("invalid")
        sut.onPositiveClicked()
        assertFalse(pluginRepositoryUrlMock.value == "invalid")
        coVerify(exactly = 1) {
            navigationMock.navigateBack()
        }
        sut.setUrl("https://google.com")
        sut.onPositiveClicked()
        assertTrue(pluginRepositoryUrlMock.value == "https://google.com")
        coVerify(exactly = 2) {
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
        sut.setUrl("https://google.com")
        sut.onNeutralClicked()
        coVerify {
            navigationMock.navigateBack()
        }
        assertTrue(pluginRepositoryUrlMock.value.isEmpty())
    }

}