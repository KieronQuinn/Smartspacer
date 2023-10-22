package com.kieronquinn.app.smartspacer.ui.screens.backuprestore

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class BackupRestoreViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun onBackupClicked(launcher: ActivityResultLauncher<String>)
    abstract fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>)

    abstract fun onBackupSelected(uri: Uri)
    abstract fun onRestoreSelected(uri: Uri)

}

class BackupRestoreViewModelImpl(
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null
): BackupRestoreViewModel(scope) {

    companion object {
        const val SMARTSPACER_BACKUP_FILE_TEMPLATE = "backup_%s.smartspacer"
    }

    override fun onBackupClicked(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getFilename())
    }

    override fun onRestoreClicked(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(arrayOf("*/*"))
    }

    override fun onBackupSelected(uri: Uri) {
        vmScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections.actionBackupRestoreFragmentToBackupFragment(uri))
        }
    }

    override fun onRestoreSelected(uri: Uri) {
        vmScope.launch {
            navigation.navigate(BackupRestoreFragmentDirections.actionBackupRestoreFragmentToRestoreFragment(uri))
        }
    }

    private fun getFilename(): String {
        val time = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return String.format(SMARTSPACER_BACKUP_FILE_TEMPLATE, dateTimeFormatter.format(time))
    }

}