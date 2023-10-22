package com.kieronquinn.app.smartspacer.ui.screens.setup.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility.AccessibilityPermissionActivity
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.smartspacer.utils.extensions.packageHasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class SetupWidgetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    companion object {
        const val INTENT_PIN_WIDGET = "${BuildConfig.APPLICATION_ID}.PIN_WIDGET"
    }

    abstract val state: StateFlow<State>

    abstract fun onWidgetClicked()
    abstract fun onWidgetAdded(context: Context)
    abstract fun onNextClicked(delay: Boolean = false)
    abstract fun launchAccessibility(context: Context)
    abstract fun openUrl(url: String)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val shouldSkip: Boolean,
            val hasClickedWidget: Boolean,
            val shouldShowLockscreenInfo: Boolean
        ): State()
    }

}

class SetupWidgetViewModelImpl(
    private val appWidgetRepository: AppWidgetRepository,
    private val navigation: SetupNavigation,
    context: Context,
    compatibility: CompatibilityRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    scope: CoroutineScope? = null
): SetupWidgetViewModel(scope) {

    companion object {
        private const val PERMISSION_MANAGER =
            "com.kieronquinn.app.smartspacer.permission.ACCESS_SMARTSPACER"
    }

    @VisibleForTesting
    val hasClickedWidget = MutableStateFlow(false)

    private val defaultLauncher = flow {
        emit(context.getDefaultLauncher())
    }

    private val isCompatible = flow {
        val compatible = compatibility.getCompatibilityReports()
        val defaultLauncher = defaultLauncher.first()
        val supportsPin = appWidgetRepository.supportsPinAppWidget()
        val defaultLauncherIsClient = defaultLauncher?.let {
            context.packageManager.packageHasPermission(it, PERMISSION_MANAGER)
        } ?: false
        val isServiceRunning = systemSmartspaceRepository.serviceRunning.value
        val shouldSkip = !supportsPin || (compatible.any { it.packageName == defaultLauncher } &&
                isServiceRunning) || defaultLauncherIsClient || defaultLauncher == null
        val shouldShowLockscreenInfo = !(compatible.any { it.packageName == PACKAGE_KEYGUARD } &&
                isServiceRunning)
        emit(Pair(shouldSkip, shouldShowLockscreenInfo))
    }

    override val state = combine(hasClickedWidget, isCompatible) { hasClicked, compat ->
        State.Loaded(compat.first, hasClicked, compat.second)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun onNextClicked(delay: Boolean) {
        vmScope.launch {
            if(delay){
                //Delay to not break navigation
                delay(500L)
            }
            navigation.navigate(SetupWidgetFragmentDirections.actionSetupWidgetFragmentToSetupRequirementsInfoFragment())
        }
    }

    override fun launchAccessibility(context: Context) {
        vmScope.launch {
            navigation.navigate(Intent(context, AccessibilityPermissionActivity::class.java))
        }
    }

    override fun onWidgetClicked() {
        vmScope.launch {
            appWidgetRepository.requestPinAppWidget(INTENT_PIN_WIDGET)
        }
    }

    override fun onWidgetAdded(context: Context) {
        vmScope.launch {
            hasClickedWidget.emit(true)
        }
    }

    override fun openUrl(url: String) {
        vmScope.launch {
            navigation.navigate(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

    private fun setupNewWidgetCallback() = vmScope.launch {
        appWidgetRepository.newAppWidgetIdBus.collect {
            val defaultLauncher = defaultLauncher.first() ?: return@collect
            appWidgetRepository.addWidget(
                it,
                defaultLauncher,
                UiSurface.HOMESCREEN,
                TintColour.AUTOMATIC,
                multiPage = true,
                showControls = true
            )
        }
    }

    init {
        setupNewWidgetCallback()
    }

}