package com.kieronquinn.app.smartspacer.ui.screens.setup.plugins

import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import com.kieronquinn.app.smartspacer.ui.screens.setup.container.SetupContainerFragmentDirections
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupPluginsViewModelTests: BaseTest<SetupPluginsViewModel>() {

    private val navigationMock = mock<RootNavigation>()

    override val sut by lazy {
        SetupPluginsViewModelImpl(navigationMock, scope)
    }

    @Test
    fun testOnFinishClicked() = runTest {
        sut.onFinishClicked()
        coVerify {
            navigationMock.navigate(
                SetupContainerFragmentDirections.actionSetupContainerFragmentToSetupCompleteFragment()
            )
        }
    }

}