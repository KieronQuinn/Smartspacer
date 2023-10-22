package com.kieronquinn.app.smartspacer.ui.screens.contributors

import android.content.Intent
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ContributorsViewModelTests: BaseTest<ContributorsViewModel>() {

    private val navigationMock = mock<ContainerNavigation>()

    override val sut by lazy {
        ContributorsViewModelImpl(navigationMock, scope)
    }

    @Test
    fun testOnLinkClicked() = runTest {
        sut.onLinkClicked("https://example.com")
        coVerify {
            navigationMock.navigate(any<Intent>())
        }
    }

}