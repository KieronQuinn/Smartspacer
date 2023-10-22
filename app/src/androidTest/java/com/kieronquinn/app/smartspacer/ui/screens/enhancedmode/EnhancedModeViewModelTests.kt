package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityState
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.EnhancedModeViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EnhancedModeViewModelTests: BaseTest<EnhancedModeViewModel>() {

    private val compatibilityRepositoryMock = mock<CompatibilityRepository> {
        coEvery { getCompatibilityState(true) } returns CompatibilityState(
            systemSupported = true,
            pixelLauncherSupported = true,
            lockscreenSupported = true,
            appPredictionSupported = true,
            oemSmartspaceSupported = true
        )
    }

    private val enhancedModeMock = mockSmartspacerSetting(false)

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { enhancedMode } returns enhancedModeMock
    }

    private val setupNavigationMock = mock<SetupNavigation>()
    private val navigationMock = mock<ContainerNavigation>()
    private val rootNavigationMock = mock<RootNavigation>()

    override val sut by lazy {
        EnhancedModeViewModelImpl(
            compatibilityRepositoryMock,
            settingsRepositoryMock,
            setupNavigationMock,
            navigationMock,
            rootNavigationMock,
            scope
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.enabled)
            enhancedModeMock.emit(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.enabled)
        }
    }

    @Test
    fun testOnSwitchClicked() = runTest {
        enhancedModeMock.emit(false)
        sut.onSwitchClicked(contextMock, true)
        coVerify {
            setupNavigationMock.navigate(
                R.id.action_enhancedModeFragment_to_enhancedModeRequestFragment, any()
            )
        }
        sut.onSwitchClicked(contextMock, false)
        coVerify {
            navigationMock.navigate(
                R.id.action_enhancedModeFragment2_to_enhancedModeRequestFragment2, any()
            )
        }
    }

    @Test
    fun testOnBackPressed() = runTest {
        sut.onBackPressed(true)
        coVerify {
            rootNavigationMock.navigateBack()
        }
        sut.onBackPressed(false)
        coVerify {
            navigationMock.navigateBack()
        }
    }

}