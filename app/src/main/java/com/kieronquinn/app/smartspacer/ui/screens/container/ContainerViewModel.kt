package com.kieronquinn.app.smartspacer.ui.screens.container

import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class ContainerViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val pluginUpdateCount: StateFlow<Int>
    abstract val pluginRepositoryEnabled: StateFlow<Boolean>
    abstract val showUpdateSnackbar: StateFlow<Boolean>

    abstract fun setCanShowSnackbar(showSnackbar: Boolean)
    abstract fun onUpdateClicked()
    abstract fun onUpdateDismissed()

}

class ContainerViewModelImpl(
    private val navigation: ContainerNavigation,
    settingsRepository: SmartspacerSettingsRepository,
    pluginRepository: PluginRepository,
    updateRepository: UpdateRepository,
    scope: CoroutineScope? = null
): ContainerViewModel(scope) {

    private val canShowSnackbar = MutableStateFlow(false)
    private val hasDismissedSnackbar = MutableStateFlow(false)

    private val gitHubUpdate = flow {
        emit(updateRepository.getUpdate())
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override val showUpdateSnackbar = combine(
        canShowSnackbar,
        gitHubUpdate,
        hasDismissedSnackbar
    ){ canShow, update, dismissed ->
        canShow && update != null && !dismissed
    }.stateIn(vmScope, SharingStarted.Eagerly, false)

    private val pluginRepositoryEnabledSetting = settingsRepository.pluginRepositoryEnabled

    override val pluginUpdateCount = pluginRepository.getUpdateCount()
        .stateIn(vmScope, SharingStarted.Eagerly, 0)

    override val pluginRepositoryEnabled = pluginRepositoryEnabledSetting.asFlow()
        .stateIn(vmScope, SharingStarted.Eagerly, pluginRepositoryEnabledSetting.getSync())

    override fun setCanShowSnackbar(showSnackbar: Boolean) {
        vmScope.launch {
            canShowSnackbar.emit(showSnackbar)
        }
    }

    override fun onUpdateClicked() {
        val release = gitHubUpdate.value ?: return
        vmScope.launch {
            navigation.navigate(
                R.id.action_global_updateFragment, bundleOf("release" to release)
            )
        }
    }

    override fun onUpdateDismissed() {
        vmScope.launch {
            hasDismissedSnackbar.emit(true)
        }
    }

}