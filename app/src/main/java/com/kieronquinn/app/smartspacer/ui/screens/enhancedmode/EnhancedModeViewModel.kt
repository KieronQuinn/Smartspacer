package com.kieronquinn.app.smartspacer.ui.screens.enhancedmode

import android.content.Context
import android.widget.Toast
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityState
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class EnhancedModeViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onSwitchClicked(context: Context, isSetup: Boolean)
    abstract fun onSkipClicked()
    abstract fun onBackPressed(isSetup: Boolean): Boolean

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val compatibilityState: CompatibilityState
        ): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

}

class EnhancedModeViewModelImpl(
    compatibilityRepository: CompatibilityRepository,
    settingsRepository: SmartspacerSettingsRepository,
    private val setupNavigation: SetupNavigation,
    private val navigation: ContainerNavigation,
    private val rootNavigation: RootNavigation,
    scope: CoroutineScope? = null
): EnhancedModeViewModel(scope) {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())
    private val enabled = settingsRepository.enhancedMode

    override val state = combine(
        enabled.asFlow(),
        reloadBus
    ) { enabled, _ ->
        State.Loaded(enabled, compatibilityRepository.getCompatibilityState(true))
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onSwitchClicked(context: Context, isSetup: Boolean) {
        vmScope.launch {
            if(enabled.get()){
                disable(context)
            }else{
                //Force set the switch back to disabled
                reloadBus.emit(System.currentTimeMillis())
                //Navigate to the check page
                enable(isSetup)
            }
        }
    }

    override fun onSkipClicked() {
        vmScope.launch {
            setupNavigation.navigate(
                R.id.action_enhancedModeFragment_to_setupTargetsFragment
            )
        }
    }

    private suspend fun enable(isSetup: Boolean) {
        val arguments = bundleOf("is_setup" to isSetup)
        if(isSetup){
            setupNavigation.navigate(
                R.id.action_enhancedModeFragment_to_enhancedModeRequestFragment, arguments
            )
        }else{
            navigation.navigate(
                R.id.action_enhancedModeFragment2_to_enhancedModeRequestFragment2, arguments
            )
        }
    }

    private suspend fun disable(context: Context) {
        //We can safely disable the setting without checks
        enabled.set(false)
        Toast.makeText(
            context, R.string.enhanced_mode_disable_toast, Toast.LENGTH_LONG
        ).show()
    }

    override fun onBackPressed(isSetup: Boolean): Boolean {
        vmScope.launch {
            if(isSetup) {
                rootNavigation.navigateBack()
            }else{
                navigation.navigateBack()
            }
        }
        return true
    }

}