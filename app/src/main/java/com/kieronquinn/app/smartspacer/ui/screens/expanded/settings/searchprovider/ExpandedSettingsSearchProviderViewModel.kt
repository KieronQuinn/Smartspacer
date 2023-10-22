package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings.searchprovider

import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class ExpandedSettingsSearchProviderViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onSearchAppClicked(searchApp: SearchApp)

    sealed class State {
        object Loading: State()
        data class Loaded(val apps: List<SearchApp>, val selectedPackage: String?): State()
    }

}

class ExpandedSettingsSearchProviderViewModelImpl(
    searchRepository: SearchRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): ExpandedSettingsSearchProviderViewModel(scope) {

    private val allApps = flow {
        emit(searchRepository.getAllSearchApps())
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        allApps,
        searchRepository.expandedSearchApp
    ) { apps, selected ->
        State.Loaded(apps, selected?.packageName)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onSearchAppClicked(searchApp: SearchApp) {
        vmScope.launch {
            settingsRepository.expandedSearchPackage.set(searchApp.packageName)
        }
    }

}