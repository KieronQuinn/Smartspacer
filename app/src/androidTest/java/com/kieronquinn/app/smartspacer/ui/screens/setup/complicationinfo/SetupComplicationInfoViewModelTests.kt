package com.kieronquinn.app.smartspacer.ui.screens.setup.complicationinfo

import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupComplicationInfoViewModelTests: BaseTest<SetupComplicationInfoViewModel>() {

    private val navigationMock = mock<SetupNavigation>()

    override val sut by lazy {
        SetupComplicationInfoViewModelImpl(navigationMock, scope)
    }

    @Test
    fun testOnNextClicked() = runTest {
        sut.onNextClicked()
        coVerify {
            navigationMock.navigate(
                SetupComplicationInfoFragmentDirections.actionSetupComplicationInfoFragmentToSetupComplicationsFragment()
            )
        }
    }

}