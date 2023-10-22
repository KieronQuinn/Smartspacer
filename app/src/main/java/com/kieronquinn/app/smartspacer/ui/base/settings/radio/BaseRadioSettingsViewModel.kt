package com.kieronquinn.app.smartspacer.ui.base.settings.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseRadioSettingsViewModel<T : Enum<T>>: ViewModel() {

    protected abstract val setting: BaseSettingsRepository.SmartspacerSetting<T>

    abstract val state: StateFlow<State>

    abstract fun onSettingClicked(setting: T)

    sealed class State {
        object Loading: State()
        data class Loaded<T>(val setting: T): State()
    }

}

abstract class BaseRadioSettingsViewModelImpl<T: Enum<T>>: BaseRadioSettingsViewModel<T>() {

    final override val state by lazy {
        setting.asFlow().map {
            State.Loaded(it)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)
    }

    final override fun onSettingClicked(setting: T) {
        viewModelScope.launch {
            this@BaseRadioSettingsViewModelImpl.setting.set(setting)
        }
    }

}