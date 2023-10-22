package com.kieronquinn.app.smartspacer.ui.screens.native.settings

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TargetCountLimit
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.native.settings.NativeModeSettingsViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NativeModeSettingsViewModelTests: BaseTest<NativeModeSettingsViewModel>() {

    private val nativeHideIncompatibleMock = mockSmartspacerSetting(false)
    private val nativeUseSplitSmartspaceMock = mockSmartspacerSetting(false)
    private val nativeTargetCountLimitMock = mockSmartspacerSetting(TargetCountLimit.ONE)

    private val navigationMock = mock<ContainerNavigation>()

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { nativeHideIncompatible } returns nativeHideIncompatibleMock
        every { nativeUseSplitSmartspace } returns nativeUseSplitSmartspaceMock
        every { nativeTargetCountLimit } returns nativeTargetCountLimitMock
    }

    override val sut by lazy {
        NativeModeSettingsViewModelImpl(
            navigationMock,
            settingsRepositoryMock,
            scope
        )
    }

    @Test
    fun testOnCountLimitClicked() = runTest {
        sut.onCountLimitClicked(true)
        coVerify {
            navigationMock.navigate(
                NativeModeSettingsFragmentDirections.actionNativeModeSettingsFragmentToNativeModePageLimitFragment(true)
            )
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
            assertTrue(item.hideIncompatibleTargets)
        }
    }

    @Test
    fun testOnUseSplitSmartspaceChanged() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onUseSplitSmartspaceChanged(true)
            awaitItem()
            val item = awaitItem()
            assertTrue(item is State.Loaded)
            item as State.Loaded
            assertTrue(item.useSplitSmartspace)
        }
    }

}