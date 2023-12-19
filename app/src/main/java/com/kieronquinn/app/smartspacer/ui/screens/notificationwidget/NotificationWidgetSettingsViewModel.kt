package com.kieronquinn.app.smartspacer.ui.screens.notificationwidget

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.hasNotificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class NotificationWidgetSettingsViewModel(
    scope: CoroutineScope?
): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun onEnabledChanged(enabled: Boolean)
    abstract fun onTintColourChanged(tintColour: TintColour)
    abstract fun onNotificationsClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val enabled: Boolean,
            val notificationsEnabled: Boolean,
            val tintColour: TintColour
        ): State()
    }

}

class NotificationWidgetSettingsViewModelImpl(
    private val navigation: ContainerNavigation,
    context: Context,
    settings: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): NotificationWidgetSettingsViewModel(scope) {

    private val enabled = settings.notificationWidgetServiceEnabled
    private val tintColour = settings.notificationWidgetTintColour
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val notificationsEnabled = resumeBus.mapLatest {
        context.hasNotificationPermission()
    }

    override val state = combine(
        enabled.asFlow(),
        tintColour.asFlow(),
        notificationsEnabled
    ) { enabled, tint, notifications ->
        State.Loaded(enabled, notifications, tint)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onResume() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onEnabledChanged(enabled: Boolean) {
        vmScope.launch {
            this@NotificationWidgetSettingsViewModelImpl.enabled.set(enabled)
        }
    }

    override fun onTintColourChanged(tintColour: TintColour) {
        vmScope.launch {
            this@NotificationWidgetSettingsViewModelImpl.tintColour.set(tintColour)
        }
    }

    override fun onNotificationsClicked() {
        vmScope.launch {
            navigation.navigate(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            })
        }
    }

}