package com.kieronquinn.app.smartspacer.ui.screens.setup.expanded

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupExpandedSmartspaceViewModelTests: BaseTest<SetupExpandedSmartspaceViewModel>() {

    private val expandedEnabledMock = mockSmartspacerSetting(false)
    private val expandedOpenModeHomeMock = mockSmartspacerSetting(ExpandedOpenMode.NEVER)
    private val expandedOpenModeLockMock = mockSmartspacerSetting(ExpandedOpenMode.NEVER)

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { expandedModeEnabled } returns expandedEnabledMock
        every { expandedOpenModeHome } returns expandedOpenModeHomeMock
        every { expandedOpenModeLock } returns expandedOpenModeLockMock
    }

    private val navigationMock = mock<SetupNavigation>()

    override val sut by lazy {
        SetupExpandedSmartspaceViewModelImpl(navigationMock, settingsRepositoryMock, scope)
    }

    @Test
    fun testEnableDisable() = runTest {
        sut.state.test {
            assertFalse(awaitItem().expandedEnabled)
            assertFalse(expandedEnabledMock.get())
            sut.onEnabledChanged(true)
            assertTrue(awaitItem().expandedEnabled)
            assertTrue(expandedEnabledMock.get())
            sut.onEnabledChanged(false)
            assertFalse(awaitItem().expandedEnabled)
            assertFalse(expandedEnabledMock.get())
        }
    }

    @Test
    fun testExpandedEnableDisable() = runTest {
        sut.state.test {
            assertFalse(awaitItem().expandedOpenEnabled)
            assertEquals(ExpandedOpenMode.NEVER, expandedOpenModeHomeMock.get())
            assertEquals(ExpandedOpenMode.NEVER, expandedOpenModeLockMock.get())
            sut.onExpandedOpenModeChanged(true)
            assertTrue(awaitItem().expandedOpenEnabled)
            assertEquals(ExpandedOpenMode.IF_HAS_EXTRAS, expandedOpenModeHomeMock.get())
            assertEquals(ExpandedOpenMode.IF_HAS_EXTRAS, expandedOpenModeLockMock.get())
            sut.onExpandedOpenModeChanged(false)
            assertFalse(awaitItem().expandedOpenEnabled)
            assertEquals(ExpandedOpenMode.NEVER, expandedOpenModeHomeMock.get())
            assertEquals(ExpandedOpenMode.NEVER, expandedOpenModeLockMock.get())
        }
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupExpandedSmartspaceFragmentDirections.actionSetupExpandedSmartspaceFragmentToSetupPluginsFragment()
            )
        }
    }

}