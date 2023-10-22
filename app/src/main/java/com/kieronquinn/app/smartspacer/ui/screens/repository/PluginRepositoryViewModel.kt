package com.kieronquinn.app.smartspacer.ui.screens.repository

import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.PluginRepository
import com.kieronquinn.app.smartspacer.repositories.PluginRepository.Plugin
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class PluginRepositoryViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setSelectedTab(index: Int)
    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String

    abstract fun reload(force: Boolean)
    abstract fun onPluginClicked(plugin: Plugin)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val items: List<Plugin>,
            val isEmpty: Boolean,
            val shouldOfferBrowse: Boolean,
            val isSearchEmpty: Boolean,
            val isAvailableTab: Boolean
        ): State()
    }

}

class PluginRepositoryViewModelImpl(
    private val navigation: ContainerNavigation,
    private val pluginRepository: PluginRepository,
    scope: CoroutineScope? = null
): PluginRepositoryViewModel(scope) {

    private val isAvailableTab = MutableStateFlow(false)
    private val isReloading = MutableStateFlow(false)

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val showSearchClear = searchTerm.mapLatest {
        it.isNotBlank()
    }.stateIn(vmScope, SharingStarted.Eagerly, false)

    private val plugins = pluginRepository.getPlugins().onEach {
        isReloading.emit(false)
    }

    override val state = combine(
        plugins,
        searchTerm,
        isAvailableTab,
        isReloading
    ) { plugins, term, available, reloading ->
        if(reloading) return@combine State.Loading
        val filteredPlugins = plugins.filter {
            it.name.toString().contains(term, true) ||
                    it.packageName.contains(term, true)
        }
        val items = ArrayList<Plugin>()
        if(!available){
            val pluginsWithUpdates = filteredPlugins.filter {
                it is Plugin.Remote && it.updateAvailable
            }.sortedBy {
                it.name.toString().lowercase()
            }
            val pluginsWithoutUpdates = filteredPlugins.filterNot {
                it is Plugin.Remote && it.updateAvailable
            }.sortedBy {
                it.name.toString().lowercase()
            }
            items.addAll(pluginsWithUpdates)
            items.addAll(pluginsWithoutUpdates)
        }else{
            val notInstalledPlugins = filteredPlugins.filterNot {
                it.isInstalled
            }
            val recommendedPlugins = notInstalledPlugins.filter {
                it is Plugin.Remote && it.recommendedApps.isNotEmpty()
            }.sortedBy {
                it.name.toString().lowercase()
            }
            val allPlugins = notInstalledPlugins.filterNot {
                it is Plugin.Remote && it.recommendedApps.isNotEmpty()
            }.sortedBy {
                it.name.toString().lowercase()
            }
            items.addAll(recommendedPlugins)
            items.addAll(allPlugins)
        }
        val shouldOfferBrowse = !available && term.isBlank() && items.isEmpty()
        State.Loaded(items, items.isEmpty(), shouldOfferBrowse, term.isBlank(), available)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setSelectedTab(index: Int) {
        vmScope.launch {
            isAvailableTab.emit(index == 1)
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term.trim())
        }
    }

    override fun onPluginClicked(plugin: Plugin) {
        vmScope.launch {
            if(plugin is Plugin.Local){
                navigation.navigate(plugin.launchIntent)
            }else{
                navigation.navigate(PluginRepositoryFragmentDirections.actionPluginRepositoryFragmentToPluginDetailsFragment(plugin))
            }
        }
    }

    override fun reload(force: Boolean) {
        vmScope.launch {
            if(force){
                isReloading.emit(true)
            }
            pluginRepository.reload(force)
        }
    }

}