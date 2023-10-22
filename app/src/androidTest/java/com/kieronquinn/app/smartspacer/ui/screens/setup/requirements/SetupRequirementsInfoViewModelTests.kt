package com.kieronquinn.app.smartspacer.ui.screens.setup.requirements

import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupRequirementsInfoViewModelTests: BaseTest<SetupRequirementsInfoViewModel>() {

    private val navigationMock = mock<SetupNavigation>()

    override val sut by lazy {
        SetupRequirementsInfoViewModelImpl(navigationMock, scope)
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupRequirementsInfoFragmentDirections.actionSetupRequirementsInfoFragmentToSetupExpandedSmartspaceFragment()
            )
        }
    }

}