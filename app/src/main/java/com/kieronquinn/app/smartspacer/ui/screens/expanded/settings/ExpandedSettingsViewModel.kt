package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings

import android.os.Build
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class ExpandedSettingsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onShowSearchBoxChanged(enabled: Boolean)
    abstract fun onSearchProviderClicked()
    abstract fun onShowDoodleChanged(enabled: Boolean)
    abstract fun onTintColourChanged(tintColour: TintColour)
    abstract fun onOpenModeHomeClicked(isFromSettings: Boolean)
    abstract fun onOpenModeLockClicked(isFromSettings: Boolean)
    abstract fun onCloseWhenLockedChanged(enabled: Boolean)
    abstract fun onBackgroundBlurChanged(enabled: Boolean)
    abstract fun onUseGoogleSansChanged(enabled: Boolean)

    abstract fun isBackgroundBlurCompatible(): Boolean

    sealed class State {
        object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val showSearchBox: Boolean,
            val searchProvider: SearchApp?,
            val showDoodle: Boolean,
            val tintColour: TintColour,
            val openModeHome: ExpandedOpenMode,
            val openModeLock: ExpandedOpenMode,
            val closeWhenLocked: Boolean,
            val backgroundBlurEnabled: Boolean,
            val widgetsUseGoogleSans: Boolean
        ): State()
    }

}

class ExpandedSettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    settings: SmartspacerSettingsRepository,
    searchRepository: SearchRepository,
    scope: CoroutineScope? = null
): ExpandedSettingsViewModel(scope) {

    private val enabled = settings.expandedModeEnabled
    private val showSearchBox = settings.expandedShowSearchBox
    private val searchApp = searchRepository.expandedSearchApp
    private val showDoodle = settings.expandedShowDoodle
    private val tintColour = settings.expandedTintColour
    private val openModeHome = settings.expandedOpenModeHome
    private val openModeLock = settings.expandedOpenModeLock
    private val closeWhenLocked = settings.expandedCloseWhenLocked
    private val backgroundBlur = settings.expandedBlurBackground
    private val widgetsUseGoogleSans = settings.expandedWidgetUseGoogleSans

    private val openMode = combine(
        openModeHome.asFlow(),
        openModeLock.asFlow()
    ) { home, lock ->
        Pair(home, lock)
    }

    private val searchOptions = combine(
        showSearchBox.asFlow(),
        searchApp,
        showDoodle.asFlow()
    ) { show, provider, doodle ->
        Triple(show, provider, doodle)
    }

    private val options = combine(
        tintColour.asFlow(),
        closeWhenLocked.asFlow(),
        backgroundBlur.asFlow(),
        widgetsUseGoogleSans.asFlow()
    ) { tint, close, blur, googleSans ->
        Options(tint, close, blur, googleSans)
    }

    override val state = combine(
        enabled.asFlow(),
        openMode,
        searchOptions,
        options,
    ) { isExpanded, open, search, options ->
        State.Loaded(
            isExpanded,
            search.first,
            search.second,
            search.third,
            options.tintColour,
            open.first,
            open.second,
            options.closeWhenLocked,
            options.backgroundBlur,
            options.widgetsUseGoogleSans
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            this@ExpandedSettingsViewModelImpl.enabled.set(enabled)
        }
    }

    override fun onSearchProviderClicked() {
        vmScope.launch {
            navigation.navigate(ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedSettingsSearchProviderFragment())
        }
    }

    override fun onShowDoodleChanged(enabled: Boolean) {
        vmScope.launch {
            showDoodle.set(enabled)
        }
    }

    override fun onTintColourChanged(tintColour: TintColour) {
        vmScope.launch {
            this@ExpandedSettingsViewModelImpl.tintColour.set(tintColour)
        }
    }

    override fun onShowSearchBoxChanged(enabled: Boolean) {
        vmScope.launch {
            showSearchBox.set(enabled)
        }
    }

    override fun onOpenModeHomeClicked(isFromSettings: Boolean) {
        vmScope.launch {
            navigation.navigate(ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedHomeOpenModeSettingsFragment(isFromSettings))
        }
    }

    override fun onOpenModeLockClicked(isFromSettings: Boolean) {
        vmScope.launch {
            navigation.navigate(ExpandedSettingsFragmentDirections.actionExpandedSettingsFragmentToExpandedLockOpenModeSettingsFragment(isFromSettings))
        }
    }

    override fun onCloseWhenLockedChanged(enabled: Boolean) {
        vmScope.launch {
            closeWhenLocked.set(enabled)
        }
    }

    override fun onBackgroundBlurChanged(enabled: Boolean) {
        vmScope.launch {
            backgroundBlur.set(enabled)
        }
    }

    override fun onUseGoogleSansChanged(enabled: Boolean) {
        vmScope.launch {
            widgetsUseGoogleSans.set(enabled)
        }
    }

    override fun isBackgroundBlurCompatible(): Boolean {
        return Build.VERSION.SDK_INT >= 30
    }

    data class Options(
        val tintColour: TintColour,
        val closeWhenLocked: Boolean,
        val backgroundBlur: Boolean,
        val widgetsUseGoogleSans: Boolean
    )

}