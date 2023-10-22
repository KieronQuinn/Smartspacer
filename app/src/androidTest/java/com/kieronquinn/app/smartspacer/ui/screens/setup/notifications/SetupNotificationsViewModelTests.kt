package com.kieronquinn.app.smartspacer.ui.screens.setup.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupNotificationsViewModelTests: BaseTest<SetupNotificationsViewModel>() {

    private val navigationMock = mock<SetupNavigation>()
    private val rootNavigationMock = mock<RootNavigation>()
    private val compatibilityRepositoryMock = mock<CompatibilityRepository>()

    override val sut by lazy {
        SetupNotificationsViewModelImpl(
            navigationMock,
            rootNavigationMock,
            compatibilityRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every {
            checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED
    }

    @Test
    fun testCheckPermission() = runTest {
        coEvery { compatibilityRepositoryMock.isEnhancedModeAvailable() } returns false
        sut.checkPermission(contextMock)
        coVerify {
            navigationMock.navigate(
                SetupNotificationsFragmentDirections.actionSetupNotificationsFragmentToSetupTargetsFragment()
            )
        }
        coEvery { compatibilityRepositoryMock.isEnhancedModeAvailable() } returns true
        sut.checkPermission(contextMock)
        coVerify {
            navigationMock.navigate(
                SetupNotificationsFragmentDirections.actionSetupNotificationsFragmentToEnhancedModeFragment(true)
            )
        }
    }

    @Test
    fun testOnPermissionResult() = runTest {
        sut.onPermissionResult(contextMock, false)
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOnGrantClicked() = runTest {
        val launcher = mock<ActivityResultLauncher<String>>()
        sut.onGrantClicked(launcher)
        verify {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

}