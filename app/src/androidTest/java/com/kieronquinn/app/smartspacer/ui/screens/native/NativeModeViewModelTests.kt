package com.kieronquinn.app.smartspacer.ui.screens.native

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.native.NativeModeViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.mockSmartspacerSetting
import com.kieronquinn.app.smartspacer.utils.randomInt
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NativeModeViewModelTests: BaseTest<NativeModeViewModel>() {

    private val setupNavigationMock = mock<SetupNavigation>()
    private val navigationMock = mock<ContainerNavigation>()

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository> {
        every { enhancedMode } returns mockSmartspacerSetting(true)
    }

    private val shizukuServiceRepositoryMock = mockShizukuRepository {

    }

    private val compatibilityRepositoryMock = mock<CompatibilityRepository> {
        coEvery { getCompatibilityReports() } returns listOf(
            CompatibilityReport(randomString(), randomInt(), emptyList())
        )
    }

    private val systemSmartspaceRepositoryMock = mock<SystemSmartspaceRepository> {
        every { serviceRunning } returns MutableStateFlow(true)
    }

    override fun setup() {
        super.setup()
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(true)
    }

    override fun Context.context() {
        every {
            packageManagerMock.getPackageInfo(any<String>(), any<PackageInfoFlags>())
        } returns PackageInfo()
        every {
            packageManagerMock.getLaunchIntentForPackage(any())
        } returns Intent()
    }

    override val sut by lazy {
        NativeModeViewModelImpl(
            systemSmartspaceRepositoryMock,
            setupNavigationMock,
            navigationMock,
            shizukuServiceRepositoryMock,
            settingsRepositoryMock,
            compatibilityRepositoryMock,
            scope
        )
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val item = expectMostRecentItem() as State.Loaded
            assertTrue(item.isEnabled)
            assertTrue(item.shizukuReady)
        }
    }

    @Test
    fun testOnSwitchClicked() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onSwitchClicked()
            coVerify {
                systemSmartspaceRepositoryMock.resetService()
            }
            coVerify {
                settingsRepositoryMock.hasUsedNativeMode.set(false)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnOpenShizukuClicked() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            sut.onOpenShizukuClicked(contextMock, false)
            coVerify {
                navigationMock.navigate(any<Intent>())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            setupNavigationMock.navigate(
                R.id.action_nativeModeFragment_to_setupBatteryOptimisationFragment
            )
        }
    }

    @Test
    fun testOnSettingsClicked() = runTest {
        sut.onSettingsClicked(true)
        coVerify {
            navigationMock.navigate(
                R.id.action_nativeModeFragment2_to_nativeModeSettingsFragment,
                any()
            )
        }
    }

    @Test
    fun testDismiss() = runTest {
        sut.dismiss()
        coVerify {
            navigationMock.navigateBack()
        }
    }

}