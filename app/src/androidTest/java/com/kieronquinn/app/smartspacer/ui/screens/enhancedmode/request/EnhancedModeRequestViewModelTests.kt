package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request.EnhancedModeRequestViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EnhancedModeRequestViewModelTests: BaseTest<EnhancedModeRequestViewModel>() {

    private val shizukuServiceRepositoryMock = mockShizukuRepository {
        every { ping() } returns true
    }

    private val setupNavigationMock = mock<SetupNavigation>()
    private val containerNavigationMock = mock<ContainerNavigation>()
    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()

    override val sut by lazy {
        EnhancedModeRequestViewModelImpl(
            contextMock,
            shizukuServiceRepositoryMock,
            setupNavigationMock,
            containerNavigationMock,
            settingsRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every {
            packageManagerMock.getPackageInfo(any<String>(), any<PackageInfoFlags>())
        } returns PackageInfo()
    }

    override fun setup() {
        super.setup()
        every { shizukuServiceRepositoryMock.isReady } returns MutableStateFlow(true)
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Result>()
            val result = expectMostRecentItem() as State.Result
            assertTrue(result.granted)
        }
    }

    @Test
    fun testOnGetShizukuClicked() = runTest {
        sut.onGetShizukuClicked(contextMock, true)
        coVerify {
            setupNavigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnGetSuiClicked() = runTest {
        sut.onGetSuiClicked(true)
        coVerify {
            setupNavigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnOpenShizukuClicked() = runTest {
        sut.onOpenShizukuClicked(true)
        coVerify {
            setupNavigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnGranted() = runTest {
        sut.onGranted(true)
        coVerify {
            setupNavigationMock.navigate(
                EnhancedModeRequestFragmentDirections.actionEnhancedModeRequestFragmentToSetupTargetsFragment()
            )
        }
        sut.onGranted(false)
        coVerify {
            containerNavigationMock.navigateUpTo(R.id.settingsFragment)
        }
    }

    @Test
    fun testOnDenied() = runTest {
        sut.onDenied(true)
        coVerify {
            setupNavigationMock.navigateBack()
        }
        sut.onDenied(false)
        coVerify {
            containerNavigationMock.navigateUpTo(R.id.settingsFragment)
        }
    }

}