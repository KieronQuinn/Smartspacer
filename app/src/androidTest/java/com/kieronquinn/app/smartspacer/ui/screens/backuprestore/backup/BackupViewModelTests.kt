package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.backup

import android.net.Uri
import app.cash.turbine.test
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingTargetsBackup
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.WritingFile
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import io.mockk.every
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackupViewModelTests: BaseTest<BackupViewModel>() {

    private val navigationMock = mock<ContainerNavigation>()
    private val backupRepositoryMock = mock<BackupRepository>()

    override val sut by lazy {
        BackupViewModelImpl(navigationMock, backupRepositoryMock, scope)
    }

    @Test
    fun testState() = runTest {
        val backupProgress = MutableStateFlow<SmartspacerBackupProgress>(
            CreatingTargetsBackup(0)
        )
        every { backupRepositoryMock.createBackup(any()) } returns backupProgress
        sut.state.test {
            assertTrue(awaitItem() == CreatingTargetsBackup(0))
            val uri = mock<Uri>()
            sut.setupWithUri(uri)
            backupProgress.emit(CreatingTargetsBackup(50))
            assertTrue(awaitItem() == CreatingTargetsBackup(50))
            backupProgress.emit(WritingFile)
            assertTrue(awaitItem() == WritingFile)
        }
    }

    @Test
    fun testOnCloseClicked() = runTest {
        sut.onCloseClicked()
        coVerify(exactly = 1) {
            navigationMock.navigateBack()
        }
    }

}