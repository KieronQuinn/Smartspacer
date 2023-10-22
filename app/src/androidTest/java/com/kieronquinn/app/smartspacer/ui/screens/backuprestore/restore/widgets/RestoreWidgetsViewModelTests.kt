package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.widgets

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestoreWidgetsViewModelTests: BaseTest<RestoreWidgetsViewModel>() {

    private val expandedRepositoryMock = mock<ExpandedRepository>()
    private val navigationMock = mock<ContainerNavigation>()

    override val sut by lazy {
        RestoreWidgetsViewModelImpl(
            expandedRepositoryMock,
            navigationMock,
            scope
        )
    }

    @Test
    fun testSetupWithConfig() = runTest {
        val mockSettings = hashMapOf("key" to "value")
        val mockBackup = mock<BackupRepository.SmartspacerBackup> {
            every { settings } returns mockSettings
        }
        val config = mock<BackupRepository.RestoreConfig> {
            every { backup } returns mockBackup
            every { shouldRestoreSettings } returns true
        }
        sut.config.test {
            assertTrue(awaitItem() == null)
            sut.setupWithConfig(config)
            assertTrue(awaitItem() == config)
            coVerify {
                expandedRepositoryMock.restoreExpandedCustomWidgetBackups(any())
            }
            coVerify {
                navigationMock.navigate(
                    RestoreWidgetsFragmentDirections.actionRestoreWidgetsFragmentToRestoreSettingsFragment(config)
                )
            }
        }
    }

}