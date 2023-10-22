package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.backup

import android.net.Uri
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.SmartspacerBackupProgress.CreatingTargetsBackup
import com.kieronquinn.app.smartspacer.ui.base.StateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BackupViewModel(
    scope: CoroutineScope?
): StateViewModel<SmartspacerBackupProgress>(scope) {

    abstract fun onCloseClicked()
    abstract fun setupWithUri(uri: Uri)

}

class BackupViewModelImpl(
    private val navigation: ContainerNavigation,
    backupRepository: BackupRepository,
    scope: CoroutineScope? = null
): BackupViewModel(scope) {

    private val backupUri = MutableStateFlow<Uri?>(null)

    override val state = backupUri.filterNotNull().flatMapLatest {
        backupRepository.createBackup(it)
    }.stateIn(vmScope, SharingStarted.Eagerly, CreatingTargetsBackup(0))

    override fun onCloseClicked() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun setupWithUri(uri: Uri) {
        vmScope.launch {
            backupUri.emit(uri)
        }
    }

}