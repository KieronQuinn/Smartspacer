package com.kieronquinn.app.smartspacer.ui.screens.notificationwidget

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.notificationwidget.NotificationWidgetSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationWidgetSettingsViewModelTests: BaseTest<NotificationWidgetSettingsViewModel>() {

    private val navigationMock = mock<ContainerNavigation>()
    private val enabledSettingMock = mockSmartspacerSetting(false)
    private val tintSettingMock = mockSmartspacerSetting(TintColour.WHITE)

    private val settingsMock = mock<SmartspacerSettingsRepository> {
        every { notificationWidgetServiceEnabled } returns enabledSettingMock
        every { notificationWidgetTintColour } returns tintSettingMock
    }

    override val sut by lazy {
        NotificationWidgetSettingsViewModelImpl(
            navigationMock,
            contextMock,
            settingsMock,
            scope
        )
    }

    @Test
    fun testEnabled() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onEnabledChanged(true)
            awaitItem()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.enabled)
        }
    }

    @Test
    fun testTint() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onTintColourChanged(TintColour.BLACK)
            awaitItem()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.tintColour == TintColour.BLACK)
        }
    }

}