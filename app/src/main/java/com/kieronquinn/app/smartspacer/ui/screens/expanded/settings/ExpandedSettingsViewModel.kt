package com.kieronquinn.app.smartspacer.ui.screens.expanded.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedSettingsProvider
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedStateProvider
import com.kieronquinn.app.smartspacer.model.expanded.NavItemDisplayMode
import com.kieronquinn.app.smartspacer.repositories.ExpandedTabRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedBackground
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
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
    abstract fun onShowDoodleChanged(enabled: Boolean)
    abstract fun onDoodleOpenGoogleAppChanged(enabled: Boolean)
    abstract fun onOpenModeHomeClicked(isFromSettings: Boolean)
    abstract fun onOpenModeLockClicked(isFromSettings: Boolean)
    abstract fun onCloseWhenLockedChanged(enabled: Boolean)
    abstract fun onBackgroundModeChanged(mode: ExpandedBackground)
    abstract fun onXposedEnabledChanged(context: Context, enabled: Boolean)
    abstract fun onNavItemDisplayModeChanged(mode: NavItemDisplayMode)
    abstract fun onShowWeatherCookieChanged(enabled: Boolean)

    abstract fun isBackgroundBlurCompatible(): Boolean

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val showDoodle: Boolean,
            val doodleOpenGoogleApp: Boolean,
            val openModeHome: ExpandedOpenMode,
            val openModeLock: ExpandedOpenMode,
            val closeWhenLocked: Boolean,
            val backgroundMode: ExpandedBackground,
            val xposedAvailable: Boolean,
            val xposedEnabled: Boolean,
            val navItemDisplayMode: NavItemDisplayMode,
            val showWeatherCookie: Boolean,
            val readYouAvailable: Boolean
        ): State()
    }

}

class ExpandedSettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    settings: SmartspacerSettingsRepository,
    private val context: Context,
    private val tabRepository: ExpandedTabRepository,
    scope: CoroutineScope? = null
): ExpandedSettingsViewModel(scope) {

    private val enabled = settings.expandedModeEnabled
    private val showDoodle = settings.expandedShowDoodle
    private val doodleOpenGoogleApp = settings.expandedDoodleOpenGoogleApp
    private val openModeHome = settings.expandedOpenModeHome
    private val openModeLock = settings.expandedOpenModeLock
    private val closeWhenLocked = settings.expandedCloseWhenLocked
    private val backgroundMode = settings.expandedBackground
    private val xposedEnabled = settings.expandedXposedEnabled
    private val showWeatherCookie = settings.expandedShowWeatherCookie

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val xposedAvailable = resumeBus.mapLatest {
        SmartspacerXposedStateProvider.getXposedEnabled(context)
    }.flowOn(Dispatchers.IO)

    private fun isReadYouAvailable(): Boolean {
        val intent = Intent().setClassName(
            "me.ash.reader",
            "me.ash.reader.infrastructure.android.MainActivity"
        )
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    // Group openMode + xposed flags to stay within 5-arg combine limit
    private val openMode = combine(
        openModeHome.asFlow(),
        openModeLock.asFlow(),
        xposedEnabled.asFlow()
    ) { home, lock, xposed -> Triple(home, lock, xposed) }

    // Group booleans + style options
    private val options = combine(
        closeWhenLocked.asFlow(),
        backgroundMode.asFlow(),
        tabRepository.navItemDisplayMode,
        showWeatherCookie.asFlow()
    ) { close, bg, nav, cookie -> object {
        val closeWhenLocked = close
        val backgroundMode = bg
        val navItemDisplayMode = nav
        val showWeatherCookie = cookie
    }}

    override val state = combine(
        enabled.asFlow(),
        openMode,
        combine(showDoodle.asFlow(), doodleOpenGoogleApp.asFlow()) { d, og -> Pair(d, og) },
        options,
        xposedAvailable
    ) { isEnabled, open, doodle, opts, xposed ->
        val readYouAvailable = isReadYouAvailable()
        State.Loaded(
            enabled = isEnabled && readYouAvailable,
            showDoodle = doodle.first,
            doodleOpenGoogleApp = doodle.second,
            openModeHome = open.first,
            openModeLock = open.second,
            closeWhenLocked = opts.closeWhenLocked,
            backgroundMode = opts.backgroundMode,
            xposedAvailable = xposed,
            xposedEnabled = open.third,
            navItemDisplayMode = opts.navItemDisplayMode,
            showWeatherCookie = opts.showWeatherCookie,
            readYouAvailable = readYouAvailable
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

    override fun onShowDoodleChanged(enabled: Boolean) {
        vmScope.launch {
            showDoodle.set(enabled)
        }
    }

    override fun onDoodleOpenGoogleAppChanged(enabled: Boolean) {
        vmScope.launch {
            doodleOpenGoogleApp.set(enabled)
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

    override fun onXposedEnabledChanged(context: Context, enabled: Boolean) {
        vmScope.launch {
            xposedEnabled.set(enabled)
            SmartspacerXposedSettingsProvider.notifyChange(context)
        }
    }

    override fun onNavItemDisplayModeChanged(mode: NavItemDisplayMode) {
        tabRepository.setNavItemDisplayMode(mode)
    }

    override fun onShowWeatherCookieChanged(enabled: Boolean) {
        vmScope.launch {
            showWeatherCookie.set(enabled)
        }
    }

    override fun isBackgroundBlurCompatible(): Boolean {
        return Build.VERSION.SDK_INT >= 30
    }

}
