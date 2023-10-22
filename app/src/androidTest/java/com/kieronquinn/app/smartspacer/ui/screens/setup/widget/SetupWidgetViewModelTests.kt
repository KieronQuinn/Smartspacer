package com.kieronquinn.app.smartspacer.ui.screens.setup.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.ResolveInfoFlags
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.setup.widget.SetupWidgetViewModel.State
import com.kieronquinn.app.smartspacer.utils.assertOutputs
import com.kieronquinn.app.smartspacer.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupWidgetViewModelTests: BaseTest<SetupWidgetViewModel>() {

    private val appWidgetRepositoryMock = mock<AppWidgetRepository>()
    private val navigationMock = mock<SetupNavigation>()

    private val systemSmartspaceRepositoryMock = mock<SystemSmartspaceRepository> {
        every { serviceRunning } returns MutableStateFlow(false)
    }

    private val compatibilityMock = mock<CompatibilityRepository> {
        coEvery { getCompatibilityReports() } returns emptyList()
    }

    override val sut by lazy {
        SetupWidgetViewModelImpl(
            appWidgetRepositoryMock,
            navigationMock,
            contextMock,
            compatibilityMock,
            systemSmartspaceRepositoryMock,
            scope
        )
    }

    override fun Context.context() {
        every { packageManager.resolveActivity(any(), any<ResolveInfoFlags>()) } returns null
    }

    @Test
    fun testState() = runTest {
        sut.state.test {
            sut.state.assertOutputs<State, State.Loaded>()
            val state = expectMostRecentItem() as State.Loaded
            assertFalse(!state.shouldSkip)
            assertTrue(state.shouldShowLockscreenInfo)
            assertFalse(state.hasClickedWidget)
        }
    }

    @Test
    fun testOnWidgetClicked() = runTest {
        sut.onWidgetClicked()
        verify {
            appWidgetRepositoryMock.requestPinAppWidget(SetupWidgetViewModel.INTENT_PIN_WIDGET)
        }
    }

    @Test
    fun testOnWidgetAdded() = runTest {
        sut.onWidgetAdded(contextMock)
        assertTrue(sut.hasClickedWidget.value)
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupWidgetFragmentDirections.actionSetupWidgetFragmentToSetupRequirementsInfoFragment()
            )
        }
    }

    @Test
    fun testLaunchAccessibility() = runTest {
        sut.launchAccessibility(contextMock)
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

    @Test
    fun testOpenUrl() = runTest {
        sut.openUrl(randomString())
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

}