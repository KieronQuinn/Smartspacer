package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings

import android.content.Context
import android.os.Build
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedSettingsProvider
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedStateProvider
import com.kieronquinn.app.smartspacer.repositories.SearchRepository
import com.kieronquinn.app.smartspacer.repositories.SearchRepository.SearchApp
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedHideAddButton
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class ExpandedSettingsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onShowSearchBoxChanged(enabled: Boolean)
    abstract fun onSearchProviderClicked()
    abstract fun onShowDoodleChanged(enabled: Boolean)
    abstract fun onTintColourChanged(tintColour: TintColour)
    abstract fun onOpenModeHomeClicked(isFromSettings: Boolean)
    abstract fun onOpenModeLockClicked(isFromSettings: Boolean)
    abstract fun onCloseWhenLockedChanged(enabled: Boolean)
    abstract fun onBackgroundModeChanged(mode: ExpandedBackground)
    abstract fun onUseGoogleSansChanged(enabled: Boolean)
    abstract fun onXposedEnabledChanged(context: Context, enabled: Boolean)
    abstract fun onHideAddChanged(hideAdd: ExpandedHideAddButton)
    abstract fun onMultiColumnChanged(enabled: Boolean)
    abstract fun onComplicationsFirstChanged(enabled: Boolean)

    abstract fun isBackgroundBlurCompatible(): Boolean

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val showSearchBox: Boolean,
            val searchProvider: SearchApp?,
            val showDoodle: Boolean,
            val tintColour: TintColour,
            val openModeHome: ExpandedOpenMode,
            val openModeLock: ExpandedOpenMode,
            val closeWhenLocked: Boolean,
            val backgroundMode: ExpandedBackground,
            val widgetsUseGoogleSans: Boolean,
            val xposedAvailable: Boolean,
            val xposedEnabled: Boolean,
            val hideAdd: ExpandedHideAddButton,
            val multiColumn: Boolean,
            val complicationsFirst: Boolean
        ): State()
    }

}

class ExpandedSettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    settings: SmartspacerSettingsRepository,
    searchRepository: SearchRepository,
    context: Context,
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
    private val backgroundMode = settings.expandedBackground
    private val widgetsUseGoogleSans = settings.expandedWidgetUseGoogleSans
    private val hideAddButton = settings.expandedHideAddButton
    private val xposedEnabled = settings.expandedXposedEnabled
    private val multiColumn = settings.expandedMultiColumnEnabled
    private val complicationsFirst = settings.expandedComplicationsFirst

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val xposedAvailable = resumeBus.mapLatest {
        SmartspacerXposedStateProvider.getXposedEnabled(context)
    }.flowOn(Dispatchers.IO)

    private val openMode = combine(
        openModeHome.asFlow(),
        openModeLock.asFlow(),
        xposedEnabled.asFlow()
    ) { home, lock, xposed ->
        Triple(home, lock, xposed)
    }

    private val searchOptions = combine(
        showSearchBox.asFlow(),
        searchApp,
        showDoodle.asFlow()
    ) { show, provider, doodle ->
        Triple(show, provider, doodle)
    }

    private val booleanOptions = combine(
        closeWhenLocked.asFlow(),
        widgetsUseGoogleSans.asFlow(),
        multiColumn.asFlow(),
        complicationsFirst.asFlow()
    ) { options ->
        options
    }

    private val options = combine(
        booleanOptions,
        tintColour.asFlow(),
        backgroundMode.asFlow(),
        hideAddButton.asFlow()
    ) { booleanOptions, tint, background, hideAdd ->
        Options(
            tint,
            booleanOptions[0],
            background,
            booleanOptions[1],
            hideAdd,
            booleanOptions[2],
            booleanOptions[3]
        )
    }

    override val state = combine(
        enabled.asFlow(),
        openMode,
        searchOptions,
        options,
        xposedAvailable
    ) { isExpanded, open, search, options, xposed ->
        State.Loaded(
            isExpanded,
            search.first,
            search.second,
            search.third,
            options.tintColour,
            open.first,
            open.second,
            options.closeWhenLocked,
            options.backgroundMode,
            options.widgetsUseGoogleSans,
            xposed,
            open.third,
            options.hideAddButton,
            options.multiColumn,
            options.complicationsFirst
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

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

    override fun onBackgroundModeChanged(mode: ExpandedBackground) {
        vmScope.launch {
            backgroundMode.set(mode)
        }
    }

    override fun onUseGoogleSansChanged(enabled: Boolean) {
        vmScope.launch {
            widgetsUseGoogleSans.set(enabled)
        }
    }

    override fun onXposedEnabledChanged(context: Context, enabled: Boolean) {
        vmScope.launch {
            xposedEnabled.set(enabled)
            SmartspacerXposedSettingsProvider.notifyChange(context)
        }
    }

    override fun onHideAddChanged(hideAdd: ExpandedHideAddButton) {
        vmScope.launch {
            hideAddButton.set(hideAdd)
        }
    }

    override fun onMultiColumnChanged(enabled: Boolean) {
        vmScope.launch {
            multiColumn.set(enabled)
        }
    }

    override fun onComplicationsFirstChanged(enabled: Boolean) {
        vmScope.launch {
            complicationsFirst.set(enabled)
        }
    }

    override fun isBackgroundBlurCompatible(): Boolean {
        return Build.VERSION.SDK_INT >= 30
    }

    data class Options(
        val tintColour: TintColour,
        val closeWhenLocked: Boolean,
        val backgroundMode: ExpandedBackground,
        val widgetsUseGoogleSans: Boolean,
        val hideAddButton: ExpandedHideAddButton,
        val multiColumn: Boolean,
        val complicationsFirst: Boolean
    )

}