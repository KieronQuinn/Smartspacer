package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.widgets

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class RestoreWidgetsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun setupWithConfig(config: RestoreConfig)

}

class RestoreWidgetsViewModelImpl(
    private val expandedRepository: ExpandedRepository,
    private val navigation: ContainerNavigation,
    scope: CoroutineScope? = null
): RestoreWidgetsViewModel(scope) {

    @VisibleForTesting
    val config = MutableStateFlow<RestoreConfig?>(null)

    override fun setupWithConfig(config: RestoreConfig) {
        vmScope.launch {
            this@RestoreWidgetsViewModelImpl.config.emit(config)
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
        expandedRepository.restoreExpandedCustomWidgetBackups(config.backup.expandedCustomWidgets)
        val directions = when {
            config.shouldRestoreSettings -> RestoreWidgetsFragmentDirections.actionRestoreWidgetsFragmentToRestoreSettingsFragment(config)
            else -> RestoreWidgetsFragmentDirections.actionRestoreWidgetsFragmentToRestoreCompleteFragment()
        }
        navigation.navigate(directions)
    }

    init {
        setupRestore()
    }

}