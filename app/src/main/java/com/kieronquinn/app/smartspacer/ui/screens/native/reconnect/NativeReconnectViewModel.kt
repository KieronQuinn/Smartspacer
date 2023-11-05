package com.kieronquinn.app.smartspacer.ui.screens.native.reconnect

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class NativeReconnectViewModel: ViewModel() {

    abstract fun onReconnectClicked()

}

class NativeReconnectViewModelImpl(
    private val configurationNavigation: ConfigurationNavigation,
    private val systemSmartspaceRepository: SystemSmartspaceRepository
): NativeReconnectViewModel() {

    override fun onReconnectClicked() {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                systemSmartspaceRepository.resetService(onlyIfAvailable = false, killSystemUi = true)
                delay(2500L)
                systemSmartspaceRepository.setService()
            }
            configurationNavigation.finishAndRemoveTask()
        }
    }

}