package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class RestoreSettingsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun setupWithConfig(config: RestoreConfig)

}

class RestoreSettingsViewModelImpl(
    private val settingsRepository: SmartspacerSettingsRepository,
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null
): RestoreSettingsViewModel(scope) {

    @VisibleForTesting
    val config = MutableStateFlow<RestoreConfig?>(null)

    override fun setupWithConfig(config: RestoreConfig) {
        vmScope.launch {
            this@RestoreSettingsViewModelImpl.config.emit(config)
        }
    }

    private fun setupRestore() {
        vmScope.launch {
            config.filterNotNull().collect {
                restore(it)
            }
        }
    }

    private suspend fun restore(config: RestoreConfig) {
        settingsRepository.restoreBackup(config.backup.settings)
        navigation.navigate(
            RestoreSettingsFragmentDirections.actionRestoreSettingsFragmentToRestoreCompleteFragment()
        )
    }

    init {
        setupRestore()
    }

}