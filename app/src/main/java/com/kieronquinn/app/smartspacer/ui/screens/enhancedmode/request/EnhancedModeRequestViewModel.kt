package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode.request

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository.ShizukuServiceResponse
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository.ShizukuServiceResponse.FailureReason
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getPlayStoreIntentForPackage
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuProvider

abstract class EnhancedModeRequestViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onGetShizukuClicked(context: Context, isSetup: Boolean)
    abstract fun onGetSuiClicked(isSetup: Boolean)
    abstract fun onOpenShizukuClicked(isSetup: Boolean)
    abstract fun onGranted(isSetup: Boolean)
    abstract fun onDenied(isSetup: Boolean)

    sealed class State {
        object Requesting: State()
        object Info: State()
        object StartShizuku: State()
        data class Result(val granted: Boolean): State()
    }

}

class EnhancedModeRequestViewModelImpl(
    context: Context,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val setupNavigation: SetupNavigation,
    private val containerNavigation: ContainerNavigation,
    private val settings: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): EnhancedModeRequestViewModel(scope) {

    private val packageManager = context.packageManager

    override val state = shizukuServiceRepository.isReady.mapLatest {
        val result = shizukuServiceRepository.runWithService {
            it.ping()
        }
        val state = when(result){
            is ShizukuServiceResponse.Success -> {
                settings.enhancedMode.set(true)
                State.Result(true)
            }
            is ShizukuServiceResponse.Failed -> {
                settings.enhancedMode.set(false)
                when {
                    result.reason == FailureReason.PERMISSION_DENIED -> {
                        State.Result(false)
                    }
                    isShizukuInstalled() -> {
                        State.StartShizuku
                    }
                    else -> {
                        State.Info
                    }
                }
            }
        }
        state
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Requesting)

    private fun isShizukuInstalled(): Boolean {
        return packageManager.isPackageInstalled(ShizukuProvider.MANAGER_APPLICATION_ID)
    }

    override fun onGetShizukuClicked(context: Context, isSetup: Boolean) {
        vmScope.launch {
            val shizukuIntent = context.getPlayStoreIntentForPackage(
                ShizukuProvider.MANAGER_APPLICATION_ID, "https://shizuku.rikka.app/download/"
            )
            if(isSetup) {
                setupNavigation.navigate(shizukuIntent ?: return@launch)
            }else{
                containerNavigation.navigate(shizukuIntent ?: return@launch)
            }
        }
    }

    override fun onGetSuiClicked(isSetup: Boolean) {
        vmScope.launch {
            val suiIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/RikkaApps/Sui")
            }
            if(isSetup) {
                setupNavigation.navigate(suiIntent)
            }else{
                containerNavigation.navigate(suiIntent)
            }
        }
    }

    override fun onOpenShizukuClicked(isSetup: Boolean) {
        vmScope.launch {
            val intent = packageManager.getLaunchIntentForPackage(
                ShizukuProvider.MANAGER_APPLICATION_ID
            ) ?: return@launch
            if(isSetup) {
                setupNavigation.navigate(intent)
            }else{
                containerNavigation.navigate(intent)
            }
        }
    }

    override fun onGranted(isSetup: Boolean) {
        vmScope.launch {
            if(isSetup){
                setupNavigation.navigate(EnhancedModeRequestFragmentDirections.actionEnhancedModeRequestFragmentToSetupTargetsFragment())
            }else{
                containerNavigation.navigateUpTo(R.id.settingsFragment)
            }
        }
    }

    override fun onDenied(isSetup: Boolean) {
        vmScope.launch {
            if(isSetup){
                setupNavigation.navigateBack()
            }else{
                containerNavigation.navigateUpTo(R.id.settingsFragment)
            }
        }
    }

}