package com.kieronquinn.app.smartspacer.ui.screens.native

import android.content.Context
import android.os.Build
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getPlayStoreIntentForPackage
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuProvider

abstract class NativeModeViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onSwitchClicked()
    abstract fun onOpenShizukuClicked(context: Context, isSetup: Boolean)
    abstract fun onNextClicked()
    abstract fun onSettingsClicked(isFromSettings: Boolean)
    abstract fun dismiss()

    sealed class State {
        object Loading: State()
        object Dismiss: State()
        data class Loaded(
            val shizukuReady: Boolean,
            val isEnabled: Boolean,
            val compatibility: List<CompatibilityReport>
        ): State()
    }

}

class NativeModeViewModelImpl(
    private val systemSmartspaceRepository: SystemSmartspaceRepository,
    private val setupNavigation: SetupNavigation,
    private val navigation: ContainerNavigation,
    shizukuServiceRepository: ShizukuServiceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    compatibilityRepository: CompatibilityRepository,
    scope: CoroutineScope? = null
): NativeModeViewModel(scope) {

    private val compatibility = flow {
        emit(compatibilityRepository.getCompatibilityReports())
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        compatibility,
        systemSmartspaceRepository.serviceRunning,
        shizukuServiceRepository.isReady
    ) { compat, running, ready ->
        val isEnhancedEnabled = settingsRepository.enhancedMode.getSync() && compat.isNotEmpty()
        if(isEnhancedEnabled) {
            State.Loaded(ready, running, compat)
        }else{
            State.Dismiss
        }
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onSwitchClicked() {
        vmScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@launch
            val isRunning = (state.value as? State.Loaded)?.isEnabled ?: return@launch
            if(isRunning){
                systemSmartspaceRepository.resetService()
                settingsRepository.hasUsedNativeMode.set(false)
            }else{
                systemSmartspaceRepository.setService()
            }
        }
    }

    override fun onOpenShizukuClicked(context: Context, isSetup: Boolean) {
        vmScope.launch {
            val isInstalled = context.packageManager.isPackageInstalled(
                ShizukuProvider.MANAGER_APPLICATION_ID
            )
            val intent = if(isInstalled){
                context.packageManager.getLaunchIntentForPackage(
                    ShizukuProvider.MANAGER_APPLICATION_ID
                )
            }else{
                context.getPlayStoreIntentForPackage(
                    ShizukuProvider.MANAGER_APPLICATION_ID,
                    "https://shizuku.rikka.app/download/"
                )
            } ?: return@launch
            if(isSetup){
                setupNavigation.navigate(intent)
            }else{
                navigation.navigate(intent)
            }
        }
    }

    override fun onNextClicked() {
        vmScope.launch {
            setupNavigation.navigate(
                R.id.action_nativeModeFragment_to_setupBatteryOptimisationFragment
            )
        }
    }

    override fun onSettingsClicked(isFromSettings: Boolean) {
        vmScope.launch {
            navigation.navigate(
                R.id.action_nativeModeFragment2_to_nativeModeSettingsFragment,
                bundleOf("is_from_settings" to isFromSettings)
            )
        }
    }

    override fun dismiss() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

}