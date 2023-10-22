package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.BackupRepository
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.LoadBackupResult
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.LoadBackupResult.ErrorReason
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class RestoreViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setupWithUri(uri: Uri)
    abstract fun onCloseClicked()

    abstract fun onRestoreTargetsChanged(enabled: Boolean)
    abstract fun onRestoreComplicationsChanged(enabled: Boolean)
    abstract fun onRestoreRequirementsChanged(enabled: Boolean)
    abstract fun onRestoreWidgetsChanged(enabled: Boolean)
    abstract fun onRestoreSettingsChanged(enabled: Boolean)

    abstract fun onNextClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val config: RestoreConfig): State()
        data class Failed(val reason: ErrorReason): State()
    }

}

class RestoreViewModelImpl(
    private val navigation: ContainerNavigation,
    backupRepository: BackupRepository
): RestoreViewModel() {

    private val backupUri = MutableStateFlow<Uri?>(null)

    private val restoreTargets = MutableStateFlow<Boolean?>(null)
    private val restoreComplications = MutableStateFlow<Boolean?>(null)
    private val restoreRequirements = MutableStateFlow<Boolean?>(null)
    private val restoreCustomWidgets = MutableStateFlow<Boolean?>(null)
    private val restoreSettings = MutableStateFlow<Boolean?>(null)

    private val config = combine(
        restoreTargets,
        restoreComplications,
        restoreRequirements,
        restoreCustomWidgets,
        restoreSettings
    ) { items ->
        items
    }

    private val backup = backupUri.filterNotNull().mapLatest {
        backupRepository.loadBackup(it)
    }

    override val state = combine(
        config,
        backup
    ) { c, b ->
        when(b){
            is LoadBackupResult.Success -> State.Loaded(b.toRestoreConfig(c))
            is LoadBackupResult.Error -> State.Failed(b.reason)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private fun LoadBackupResult.Success.toRestoreConfig(config: Array<Boolean?>): RestoreConfig {
        val hasTargets = backup.targetBackups.isNotEmpty()
        val hasComplications = backup.complicationBackups.isNotEmpty()
        val hasRequirements = backup.requirementBackups.isNotEmpty()
        val hasCustomWidgets = backup.expandedCustomWidgets.isNotEmpty()
        val hasSettings = backup.settings.isNotEmpty()
        val restoreTargets = config[0] ?: hasTargets
        val restoreComplications = config[1] ?: hasComplications
        val restoreRequirements = config[2] ?: hasRequirements
        val restoreCustomWidgets = config[3] ?: hasCustomWidgets
        val restoreSettings = config[4] ?: hasSettings
        return RestoreConfig(
            hasTargets,
            hasComplications,
            hasRequirements,
            hasCustomWidgets,
            hasSettings,
            restoreTargets,
            restoreComplications,
            restoreRequirements,
            restoreCustomWidgets,
            restoreSettings,
            backup
        )
    }

    override fun setupWithUri(uri: Uri) {
        viewModelScope.launch {
            backupUri.emit(uri)
        }
    }

    override fun onCloseClicked() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun onRestoreTargetsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreTargets.emit(enabled)
        }
    }

    override fun onRestoreComplicationsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreComplications.emit(enabled)
        }
    }

    override fun onRestoreRequirementsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreRequirements.emit(enabled)
        }
    }

    override fun onRestoreWidgetsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreCustomWidgets.emit(enabled)
        }
    }

    override fun onRestoreSettingsChanged(enabled: Boolean) {
        viewModelScope.launch {
            restoreSettings.emit(enabled)
        }
    }

    override fun onNextClicked() {
        val config = (state.value as? State.Loaded)?.config ?: return
        viewModelScope.launch {
            val directions = when{
                config.shouldRestoreTargets -> RestoreFragmentDirections.actionRestoreFragmentToRestoreTargetsFragment(config)
                config.shouldRestoreComplications -> RestoreFragmentDirections.actionRestoreFragmentToRestoreComplicationsFragment(config)
                config.shouldRestoreRequirements -> RestoreFragmentDirections.actionRestoreFragmentToRestoreRequirementsFragment(config)
                config.shouldRestoreExpandedCustomWidgets -> RestoreFragmentDirections.actionRestoreFragmentToRestoreWidgetsFragment(config)
                else -> RestoreFragmentDirections.actionRestoreFragmentToRestoreCompleteFragment()
            }
            navigation.navigate(directions)
        }
    }

}