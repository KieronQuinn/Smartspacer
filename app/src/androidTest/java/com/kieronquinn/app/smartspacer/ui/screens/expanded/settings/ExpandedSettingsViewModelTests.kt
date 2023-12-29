package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings

import android.content.Intent
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.ExpandedSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomBoolean
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpandedSettingsViewModelTests: BaseTest<ExpandedSettingsViewModel>() {

    companion object {
        private fun getMockSearchApp(): SearchApp {
            return SearchApp(
                randomString(),
                randomString(),
                null,
                randomBoolean(),
                randomBoolean(),
                Intent()
            )
        }
    }

    private val mockSearchApps = listOf(getMockSearchApp(), getMockSearchApp(), getMockSearchApp())
    private val searchAppMock = MutableStateFlow(mockSearchApps.first())
    private val enabledMock = mockSmartspacerSetting(false)
    private val showSearchBoxMock = mockSmartspacerSetting(false)
    private val showDoodleMock = mockSmartspacerSetting(false)
    private val tintColourMock = mockSmartspacerSetting(TintColour.AUTOMATIC)
    private val openModeHomeMock = mockSmartspacerSetting(ExpandedOpenMode.NEVER)
    private val openModeLockMock = mockSmartspacerSetting(ExpandedOpenMode.NEVER)
    private val closeWhenLockedMock = mockSmartspacerSetting(false)
    private val backgroundBlurMock = mockSmartspacerSetting(false)
    private val widgetsUseGoogleSansMock = mockSmartspacerSetting(false)
    private val navigationMock = mock<ContainerNavigation>()

    private val settingsMock = mock<SmartspacerSettingsRepository> {
        every { expandedModeEnabled } returns enabledMock
        every { expandedShowSearchBox } returns showSearchBoxMock
        every { expandedShowDoodle } returns showDoodleMock
        every { expandedTintColour } returns tintColourMock
        every { expandedOpenModeHome } returns openModeHomeMock
        every { expandedOpenModeLock } returns openModeLockMock
        every { expandedCloseWhenLocked } returns closeWhenLockedMock
        every { expandedBlurBackground } returns backgroundBlurMock
        every { expandedWidgetUseGoogleSans } returns widgetsUseGoogleSansMock
    }

    private val searchRepositoryMock = mock<SearchRepository> {
        every { expandedSearchApp } returns searchAppMock
    }

    override val sut by lazy {
        ExpandedSettingsViewModelImpl(
            navigationMock,
            settingsMock,
            searchRepositoryMock,
            contextMock,
            scope
        )
    }

    @Test
    fun testOnEnabledChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.enabled)
            sut.onEnabledChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.enabled)
        }
    }

    @Test
    fun testOnShowSearchBoxChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.showSearchBox)
            sut.onShowSearchBoxChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.showSearchBox)
        }
    }

    @Test
    fun testOnShowDoodleChanged() = runTest {
        sut.state.test {
            println("A")
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            println("B")
            assertFalse(item.showDoodle)
            sut.onShowDoodleChanged(true)
            val updatedItem = awaitItem()
            println("C")
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.showDoodle)
        }
    }

    @Test
    fun testOnCloseWhenLockedChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.closeWhenLocked)
            sut.onCloseWhenLockedChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.closeWhenLocked)
        }
    }

    @Test
    fun testOnBackgroundBlurChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.backgroundBlurEnabled)
            sut.onBackgroundBlurChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.backgroundBlurEnabled)
        }
    }

    @Test
    fun testOnTintColourChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertTrue(item.tintColour == TintColour.AUTOMATIC)
            sut.onTintColourChanged(TintColour.WHITE)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.tintColour == TintColour.WHITE)
        }
    }

    @Test
    fun testWidgetsUseGoogleSansChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertFalse(item.widgetsUseGoogleSans)
            sut.onUseGoogleSansChanged(true)
            val updatedItem = awaitItem()
            assertTrue(updatedItem is State.Loaded)
            updatedItem as State.Loaded
            assertTrue(updatedItem.widgetsUseGoogleSans)
        }
    }

    @Test
    fun testOnSearchProviderClicked() = runTest {
        sut.onSearchProviderClicked()
        coVerify {
            navigationMock.navigate(
                ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedSettingsSearchProviderFragment()
            )
        }
    }

    @Test
    fun testOnOpenModeHomeClicked() = runTest {
        sut.onOpenModeHomeClicked(false)
        coVerify {
            navigationMock.navigate(
                ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedHomeOpenModeSettingsFragment(false)
            )
        }
    }

    @Test
    fun testOnOpenModeLockClicked() = runTest {
        sut.onOpenModeLockClicked(false)
        coVerify {
            navigationMock.navigate(
                ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedLockOpenModeSettingsFragment(false)
            )
        }
    }
    
}