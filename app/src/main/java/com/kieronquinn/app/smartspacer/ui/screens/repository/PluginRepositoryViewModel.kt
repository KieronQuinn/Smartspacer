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

    /** No longer drives filtering — tab position is managed by ViewPager2. */
    abstract fun setSelectedTab(index: Int)
    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String

    abstract fun reload(force: Boolean)
    abstract fun onPluginClicked(plugin: Plugin)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val installedItems: List<Plugin>,
            val availableItems: List<Plugin>,
            val isSearchEmpty: Boolean
        ): State()
    }

}

class PluginRepositoryViewModelImpl(
    private val navigation: ContainerNavigation,
    private val pluginRepository: PluginRepository,
    scope: CoroutineScope? = null
): PluginRepositoryViewModel(scope) {

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
        isReloading
    ) { plugins, term, reloading ->
        if (reloading) return@combine State.Loading
        val filtered = plugins.filter {
            it.name.toString().contains(term, true) ||
                    it.packageName.contains(term, true)
        }
        State.Loaded(
            installedItems = buildInstalledList(filtered),
            availableItems = buildAvailableList(filtered),
            isSearchEmpty = term.isBlank()
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private fun buildInstalledList(filtered: List<Plugin>): List<Plugin> {
        val installed = filtered.filter { it.isInstalled }
        val withUpdates = installed.filter { it is Plugin.Remote && it.updateAvailable }
            .sortedBy { it.name.toString().lowercase() }
        val withoutUpdates = installed.filterNot { it is Plugin.Remote && it.updateAvailable }
            .sortedBy { it.name.toString().lowercase() }
        return withUpdates + withoutUpdates
    }

    private fun buildAvailableList(filtered: List<Plugin>): List<Plugin> {
        val notInstalled = filtered.filterNot { it.isInstalled }
        val recommended = notInstalled.filter { it is Plugin.Remote && it.recommendedApps.isNotEmpty() }
            .sortedBy { it.name.toString().lowercase() }
        val rest = notInstalled.filterNot { it is Plugin.Remote && it.recommendedApps.isNotEmpty() }
            .sortedBy { it.name.toString().lowercase() }
        return recommended + rest
    }

    /** Tab position is now driven by ViewPager2; this is a no-op. */
    override fun setSelectedTab(index: Int) {}

    override fun getSearchTerm(): String = searchTerm.value

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term.trim())
        }
    }

    override fun onPluginClicked(plugin: Plugin) {
        vmScope.launch {
            if (plugin is Plugin.Local) {
                navigation.navigate(plugin.launchIntent)
            } else {
                navigation.navigate(
                    PluginRepositoryFragmentDirections
                        .actionPluginRepositoryFragmentToPluginDetailsFragment(plugin)
                )
            }
        }
    }

    override fun reload(force: Boolean) {
        vmScope.launch {
            if (force) isReloading.emit(true)
            pluginRepository.reload(force)
        }
    }

}
