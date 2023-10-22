package com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace

import android.content.Intent
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.oemsmartspace.SettingsOemSmartspaceViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SettingsOemSmartspaceViewModelTests: BaseTest<SettingsOemSmartspaceViewModel>() {

    private val oemSmartspaceEnabledMock = mockSmartspacerSetting(false)
    private val hideIncompatibleMock = mockSmartspacerSetting(false)

    private val navigationMock = mock<ContainerNavigation>()

    private val grantRepositoryMock = mock<GrantRepository> {
        every { grants } returns MutableStateFlow(emptyList())
    }

    private val shizukuServiceRepositoryMock = mockShizukuRepository(mockSui = {
        every { isCompatible } returns true
    }, mock = {

    })

    private val oemSmartspacerRepositoryMock = mock<OemSmartspacerRepository> {
        every { getCompatibleApps() } returns flowOf(emptyList())
    }

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { oemSmartspaceEnabled } returns oemSmartspaceEnabledMock
        every { oemHideIncompatible } returns hideIncompatibleMock
    }

    override val sut by lazy {
        SettingsOemSmartspaceViewModelImpl(
            navigationMock,
            grantRepositoryMock,
            oemSmartspacerRepositoryMock,
            contextMock,
            settingsRepositoryMock,
            shizukuServiceRepositoryMock,
            scope
        )
    }

    @Test
    fun testOnEnabledChanged() = runTest {
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
    fun testOnAppChanged() = runTest {
        sut.onAppChanged(randomString(), true)
        coVerify {
            grantRepositoryMock.addGrant(any())
        }
    }

    @Test
    fun testOnHideIncompatibleChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onHideIncompatibleChanged(true)
            awaitItem()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.hideIncompatible)
        }
    }

    @Test
    fun testOnIncompatibleClicked() = runTest {
        sut.onIncompatibleClicked()
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnReadMoreClicked() = runTest {
        sut.onReadMoreClicked()
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

}