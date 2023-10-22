package com.kieronquinn.app.smartspacer.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class MainActivityViewModel: ViewModel() {

    abstract val startDestination: StateFlow<Int?>

}

class MainActivityViewModelImpl(
    private val settings: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    skipSplash: Boolean
): MainActivityViewModel() {

    companion object {
        private const val SPLASH_TIMEOUT = 750L
    }

    private val hasSeenSetup = settings.hasSeenSetup.asFlow()

    private val splashTimeout = flow {
        if(!skipSplash) {
            delay(SPLASH_TIMEOUT)
        }
        emit(Unit)
    }

    override val startDestination = combine(hasSeenSetup, splashTimeout) { seenSetup, _ ->
        if(seenSetup){
            R.id.containerFragment
        }else{
            R.id.setupLandingFragment
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        systemSmartspaceRepository.showNativeStartReminderIfNeeded()
        viewModelScope.launch {
            settings.setInstallTimeIfNeeded()
        }
    }

}