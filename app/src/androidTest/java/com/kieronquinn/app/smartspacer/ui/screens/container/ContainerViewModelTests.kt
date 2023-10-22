package com.kieronquinn.app.smartspacer.ui.screens.container

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coEvery
import io.mockk.coVerify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ContainerViewModelTests: BaseTest<ContainerViewModel>() {

    private val navigationMock = mock<ContainerNavigation>()
    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val pluginRepositoryMock = mock<PluginRepository>()

    private val updateRepositoryMock = mock<UpdateRepository> {
        coEvery { getUpdate() } returns mock()
    }

    override val sut by lazy {
        ContainerViewModelImpl(
            navigationMock,
            settingsRepositoryMock,
            pluginRepositoryMock,
            updateRepositoryMock,
            scope
        )
    }

    @Test
    fun testShowUpdateSnackbar() = runTest {
        sut.showUpdateSnackbar.test {
            assertFalse(awaitItem())
            sut.setCanShowSnackbar(true)
            assertTrue(awaitItem())
            sut.onUpdateDismissed()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun testOnUpdateClicked() = runTest {
        sut.onUpdateClicked()
        coVerify {
            navigationMock.navigate(R.id.action_global_updateFragment, any())
        }
    }

}