package com.kieronquinn.app.smartspacer.ui.screens.setup.targetinfo

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupTargetInfoViewModelTests: BaseTest<SetupTargetInfoViewModel>() {

    private val navigationMock = mock<SetupNavigation>()
    private val rootNavigationMock = mock<RootNavigation>()

    override val sut by lazy {
        SetupTargetInfoViewModelImpl(navigationMock, rootNavigationMock, scope)
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupTargetInfoFragmentDirections.actionSetupTargetInfoFragmentToSetupTargetsFragment()
            )
        }
    }

    @Test
    fun testOnBackClicked() = runTest {
        sut.onBackPressed()
        coVerify {
            rootNavigationMock.navigateBack()
        }
    }

}