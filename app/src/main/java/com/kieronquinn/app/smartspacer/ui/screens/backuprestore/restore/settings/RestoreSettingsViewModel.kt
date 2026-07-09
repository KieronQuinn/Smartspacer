package com.kieronquinn.app.smartspacer.ui.screens.backuprestore.restore.settings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.expanded.ExpandedTabConfig
import com.kieronquinn.app.smartspacer.model.expanded.NavItemDisplayMode
import com.kieronquinn.app.smartspacer.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.smartspacer.repositories.BackupRepositoryImpl
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
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
    private val expandedTabRepository: ExpandedTabRepository,
    private val gson: Gson,
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
        val settings = config.backup.settings
        settingsRepository.restoreBackup(settings)
        restoreTabConfig(settings)
        navigation.navigate(
            RestoreSettingsFragmentDirections.actionRestoreSettingsFragmentToRestoreCompleteFragment()
        )
    }

    private fun restoreTabConfig(settings: Map<String, String>) {
        settings[BackupRepositoryImpl.SETTINGS_KEY_TAB_CONFIGS]?.let { json ->
            try {
                val type = object : TypeToken<List<ExpandedTabConfig>>() {}.type
                val tabs: List<ExpandedTabConfig> = gson.fromJson(json, type) ?: return@let
                expandedTabRepository.saveTabs(tabs)
            } catch (_: Exception) { }
        }
        settings[BackupRepositoryImpl.SETTINGS_KEY_NAV_DISPLAY_MODE]?.let { name ->
            try {
                expandedTabRepository.setNavItemDisplayMode(NavItemDisplayMode.valueOf(name))
            } catch (_: Exception) { }
        }
    }

    init {
        setupRestore()
    }

}