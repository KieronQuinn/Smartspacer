package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings

import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestoreSettingsViewModelTests: BaseTest<RestoreSettingsViewModel>() {

    private val settingsRepositoryMock = mock<SmartspacerSettingsRepository>()
    private val navigationMock = mock<ContainerNavigation>()

    override val sut by lazy {
        RestoreSettingsViewModelImpl(settingsRepositoryMock, navigationMock, scope)
    }

    @Test
    fun testSetupWithConfig() = runTest {
        val mockSettings = hashMapOf("key" to "value")
        val mockBackup = mock<BackupRepository.SmartspacerBackup> {
            every { settings } returns mockSettings
        }
        val config = mock<RestoreConfig> {
            every { backup } returns mockBackup
        }
        sut.config.test {
            assertTrue(awaitItem() == null)
            sut.setupWithConfig(config)
            assertTrue(awaitItem() == config)
            coVerify {
                settingsRepositoryMock.restoreBackup(any())
            }
            coVerify {
                navigationMock.navigate(
                    RestoreSettingsFragmentDirections.actionRestoreSettingsFragmentToRestoreCompleteFragment()
                )
            }
        }
    }

}