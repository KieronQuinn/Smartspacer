package com.kieronquinn.app.smartspacer.ui.screens.backuprestore

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.test.BaseTest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupRestoreViewModelTests: BaseTest<BackupRestoreViewModel>() {

    private val containerNavigationMock = mock<ContainerNavigation>()
    private val mockTime = LocalDateTime.now()

    override val sut by lazy {
        BackupRestoreViewModelImpl(containerNavigationMock, scope)
    }

    override fun setup() {
        super.setup()
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns mockTime
    }

    @Test
    fun testOnBackupClicked() = runTest {
        val launcher = mock<ActivityResultLauncher<String>>()
        sut.onBackupClicked(launcher)
        verify(exactly = 1) {
            launcher.launch(getFilename())
        }
    }

    @Test
    fun testOnRestoreClicked() = runTest {
        val launcher = mock<ActivityResultLauncher<Array<String>>>()
        sut.onRestoreClicked(launcher)
        verify(exactly = 1) {
            launcher.launch(arrayOf("*/*"))
        }
    }

    @Test
    fun testOnBackupSelected() = runTest {
        val uri = mock<Uri>()
        sut.onBackupSelected(uri)
        coVerify(exactly = 1) {
            containerNavigationMock.navigate(
                BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupFragment(uri)
            )
        }
    }

    @Test
    fun testOnRestoreSelected() = runTest {
        val uri = mock<Uri>()
        sut.onRestoreSelected(uri)
        coVerify(exactly = 1) {
            containerNavigationMock.navigate(
                BackupRestoreFragmentDirections.actionBackupRestoreFragmentToRestoreFragment(uri)
            )
        }
    }

    private fun getFilename(): String {
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(
            BackupRestoreViewModelImpl.SMARTSPACER_BACKUP_FILE_TEMPLATE,
            dateTimeFormatter.format(mockTime)
        )
    }

}