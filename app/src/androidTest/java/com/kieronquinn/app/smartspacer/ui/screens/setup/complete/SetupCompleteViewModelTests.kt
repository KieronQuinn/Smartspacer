package com.kieronquinn.app.smartspacer.ui.screens.setup.complete

import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SetupCompleteViewModelTests: BaseTest<SetupCompleteViewModel>() {

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val navigationMock = mock<RootNavigation>()

    override val sut by lazy {
        SetupCompleteViewModelImpl(
            settingsRepositoryMock,
            navigationMock
        )
    }

    @Test
    fun testOnResume() = runTest {
        sut.onResume()
        coVerify {
            settingsRepositoryMock.hasSeenSetup.set(true)
        }
    }

    @Test
    fun testOnCloseClicked() = runTest {
        sut.onCloseClicked()
        navigationMock.navigate(R.id.action_global_settings)
    }

}