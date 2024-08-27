package com.kieronquinn.app.smartspacer.ui.screens.settings.language

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.utils.extensions.getSelectedLanguage
import com.kieronquinn.app.smartspacer.utils.extensions.getSupportedLocales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

abstract class SettingsLanguageViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun reload()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val supportedLocales: List<Locale>,
            val selectedLocale: Locale?
        ): State()
    }

}

class SettingsLanguageViewModelImpl(context: Context): SettingsLanguageViewModel() {

    private val reloadBus = MutableStateFlow(System.currentTimeMillis())

    override val state = reloadBus.map {
        val supported = context.getSupportedLocales().sortedBy { it.displayName.lowercase() }
        val selected = context.getSelectedLanguage(supported.map { it.toLanguageTag() })
        State.Loaded(supported, selected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun reload() {
        viewModelScope.launch {
            reloadBus.emit(System.currentTimeMillis())
        }
    }

}