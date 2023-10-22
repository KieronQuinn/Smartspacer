package com.kieronquinn.app.smartspacer.ui.screens.repository.settings

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.repository.settings.PluginRepositorySettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginRepositorySettingsViewModelTests: BaseTest<PluginRepositorySettingsViewModel>() {

    private val pluginRepositoryEnabledMock = mockSmartspacerSetting(false)
    private val updateCheckEnabledMock = mockSmartspacerSetting(false)
    private val containerNavigationMock = mock<ContainerNavigation>()

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { pluginRepositoryEnabled } returns pluginRepositoryEnabledMock
        every { pluginRepositoryUpdateCheckEnabled } returns updateCheckEnabledMock
    }

    override val sut by lazy {
        PluginRepositorySettingsViewModelImpl(
            containerNavigationMock,
            settingsRepositoryMock,
            scope
        )
    }

    @Test
    fun testOnEnabledChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertFalse(state.enabled)
            sut.onEnabledChanged(true)
            val updatedState = awaitItem()
            assertTrue(updatedState is State.Loaded)
            updatedState as State.Loaded
            assertTrue(updatedState.enabled)
        }
    }

    @Test
    fun testOnUpdateCheckEnabledChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertFalse(state.updateCheckEnabled)
            sut.onUpdateCheckEnabledChanged(true)
            val updatedState = awaitItem()
            assertTrue(updatedState is State.Loaded)
            updatedState as State.Loaded
            assertTrue(updatedState.updateCheckEnabled)
        }
    }

    @Test
    fun testOnUrlClicked() = runTest {
        sut.onUrlClicked()
        coVerify {
            containerNavigationMock.navigate(
                PluginRepositorySettingsFragmentDirections.actionPluginRepositorySettingsFragmentToPluginRepositorySettingsUrlBottomSheetFragment()
            )
        }
    }

}