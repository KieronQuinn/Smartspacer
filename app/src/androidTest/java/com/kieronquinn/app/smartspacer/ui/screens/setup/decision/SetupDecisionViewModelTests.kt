package com.kieronquinn.app.smartspacer.ui.screens.setup.decision

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.setup.decision.SetupDecisionViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coEvery
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupDecisionViewModelTests: BaseTest<SetupDecisionViewModel>() {

    private val navigationMock = mock<SetupNavigation>()
    private val compatibilityRepositoryMock = mock<CompatibilityRepository>()
    private val notificationRepositoryMock = mock<NotificationRepository>()
    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()

    override val sut by lazy {
        SetupDecisionViewModelImpl(
            navigationMock,
            settingsRepositoryMock,
            compatibilityRepositoryMock,
            notificationRepositoryMock,
            scope
        )
    }

    @Test
    fun testStateNotifications() = runTest {
        every { notificationRepositoryMock.hasNotificationPermission() } returns false
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertTrue(
                state.directions == SetupDecisionFragmentDirections.actionSetupDecisionFragmentToSetupNotificationsFragment()
            )
        }
    }

    @Test
    fun testStateEnhanced() = runTest {
        every { notificationRepositoryMock.hasNotificationPermission() } returns true
        coEvery { compatibilityRepositoryMock.isEnhancedModeAvailable() } returns true
        every { settingsRepositoryMock.enhancedMode } returns mockSmartspacerSetting(false)
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertTrue(
                state.directions == SetupDecisionFragmentDirections.actionSetupDecisionFragmentToEnhancedModeFragment(true)
            )
        }
    }

    @Test
    fun testStateTargets() = runTest {
        every { notificationRepositoryMock.hasNotificationPermission() } returns true
        coEvery { compatibilityRepositoryMock.isEnhancedModeAvailable() } returns true
        every { settingsRepositoryMock.enhancedMode } returns mockSmartspacerSetting(true)
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertTrue(
                state.directions == SetupDecisionFragmentDirections.actionSetupDecisionFragmentToSetupTargetsFragment()
            )
        }
    }

}