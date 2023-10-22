package com.kieronquinn.app.smartspacer.ui.screens.setup.landing

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupLandingViewModelTests: BaseTest<SetupLandingViewModel>() {

    private val rootNavigationMock = mock<RootNavigation>()

    override val sut by lazy {
        SetupLandingViewModelImpl(
            rootNavigationMock,
            scope
        )
    }

    @Test
    fun testOnGetStartedClicked() = runTest {
        sut.onGetStartedClicked()
        coVerify {
            rootNavigationMock.navigate(
                SetupLandingFragmentDirections.actionSetupLandingFragmentToSetupAnalyticsFragment()
            )
        }
    }

}