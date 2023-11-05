package com.kieronquinn.app.smartspacer.ui.screens.widget

import android.content.Context
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class SmartspacerWidgetConfigurationViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val closeBus: Flow<Unit>
    abstract val state: StateFlow<State>

    abstract fun onResume()
    abstract fun setupWithAppWidget(appWidget: AppWidget)
    abstract fun onHomeClicked()
    abstract fun onLockClicked()
    abstract fun onPageSingleClicked()
    abstract fun onPageControlsClicked()
    abstract fun onPageNoControlsClicked()
    abstract fun onAnimateChanged(enabled: Boolean)
    abstract fun onColourAutomaticClicked()
    abstract fun onColourWhiteClicked()
    abstract fun onColourBlackClicked()
    abstract fun onApplyClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val isAccessibilityServiceEnabled: Boolean,
            val isPossiblyLockScreen: Boolean,
            val appWidget: AppWidget
        ): State()
        data object Close: State()
    }

}

class SmartspacerWidgetConfigurationViewModelImpl(
    context: Context,
    private val appWidgetRepository: AppWidgetRepository,
    scope: CoroutineScope? = null
): SmartspacerWidgetConfigurationViewModel(scope) {

    private val resumeBus = MutableStateFlow(System.currentTimeMillis())
    private val appWidget = MutableStateFlow<AppWidget?>(null)

    private val isAccessibilityServiceEnabled = resumeBus.mapLatest {
        context.isAccessibilityServiceRunning()
    }.stateIn(vmScope, SharingStarted.Eagerly, context.isAccessibilityServiceRunning())

    private val homePackage = resumeBus.mapLatest {
        context.getDefaultLauncher()
    }.stateIn(vmScope, SharingStarted.Eagerly, context.getDefaultLauncher())

    override val closeBus = MutableSharedFlow<Unit>()

    override val state = combine(
        appWidget.filterNotNull(),
        homePackage,
        isAccessibilityServiceEnabled
    ) { widget, home, enabled ->
        //If the owner package is not specified nor currently saved, we cannot use this widget
        if(widget.ownerPackage.isNotBlank()){
            //If the package is not the current default home, this may be a Lockscreen Widget app
            val isPossiblyLockScreen = widget.ownerPackage != home
            State.Loaded(enabled, isPossiblyLockScreen, widget)
        }else State.Close
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    private fun Context.isAccessibilityServiceRunning(): Boolean {
        return isServiceRunning(SmartspacerAccessibiltyService::class.java)
    }

    override fun setupWithAppWidget(appWidget: AppWidget) {
        if(this.appWidget.value != null) return
        vmScope.launch {
            val current = appWidgetRepository.getAppWidget(appWidget.appWidgetId)
            this@SmartspacerWidgetConfigurationViewModelImpl.appWidget.emit(current ?: appWidget)
        }
    }

    override fun onHomeClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(surface = UiSurface.HOMESCREEN))
        }
    }

    override fun onLockClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(surface = UiSurface.LOCKSCREEN))
        }
    }

    override fun onPageSingleClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(multiPage = false, showControls = false))
        }
    }

    override fun onPageControlsClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(multiPage = true, showControls = true))
        }
    }

    override fun onPageNoControlsClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(multiPage = true, showControls = false))
        }
    }

    override fun onAnimateChanged(enabled: Boolean) {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(animate = enabled))
        }
    }

    override fun onColourAutomaticClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(tintColour = TintColour.AUTOMATIC))
        }
    }

    override fun onColourWhiteClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(tintColour = TintColour.WHITE))
        }
    }

    override fun onColourBlackClicked() {
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidget.emit(current.copy(tintColour = TintColour.BLACK))
        }
    }

    override fun onResume() {
        vmScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun onApplyClicked() {
        if(!isAccessibilityServiceEnabled.value) return
        vmScope.launch {
            val current = appWidget.value ?: return@launch
            appWidgetRepository.addWidget(
                current.appWidgetId,
                current.ownerPackage,
                current.surface,
                current.tintColour,
                current.multiPage,
                current.showControls,
                current.animate
            )
            closeBus.emit(Unit)
        }
    }

}