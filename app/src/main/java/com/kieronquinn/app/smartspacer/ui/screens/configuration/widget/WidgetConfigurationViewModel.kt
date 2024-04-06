package com.kieronquinn.app.smartspacer.ui.screens.configuration.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ConfigurationNavigation
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility.AccessibilityPermissionActivity
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class WidgetConfigurationViewModel: ViewModel() {

    abstract val state: StateFlow<State>

    abstract fun setup(appWidgetId: Int, owner: String)
    abstract fun onResume()
    abstract fun commitAndClose()
    abstract fun onAccessibilityClicked(context: Context)
    abstract fun onTabChanged(index: Int)
    abstract fun onSurfaceChanged(isLockscreen: Boolean)
    abstract fun onSinglePageChanged(enabled: Boolean)
    abstract fun onShowControlsChanged(enabled: Boolean)
    abstract fun onInvisibleControlsChanged(enabled: Boolean)
    abstract fun onAnimationChanged(enabled: Boolean)
    abstract fun onTintColourChanged(tintColour: TintColour)
    abstract fun onShadowChanged(enabled: Boolean)
    abstract fun onPaddingChanged(padding: Int)

    abstract fun Context.getPagedWidget(
        appWidgetId: Int,
        session: PagedWidgetSmartspacerSessionState,
        config: AppWidget?
    ): RemoteViews?

    abstract fun Context.getPageRemoteViews(
        appWidgetId: Int,
        view: SmartspaceView,
        config: AppWidget?,
        isList: Boolean,
        overflowIntent: Intent?,
        container: (() -> RemoteViews) -> RemoteViews = { it() }
    ): RemoteViews

    abstract fun Context.getListWidget(widget: AppWidget): RemoteViews

    sealed class State {
        data object Loading: State() {
            override fun equalsForUi(other: Any?): Boolean {
                return other is Loading
            }
        }
        data class Loaded(
            val widget: AppWidget,
            val hasGrantedAccessibility: Boolean,
            val isLockScreenAvailable: Boolean
        ): State() {
            override fun equalsForUi(other: Any?): Boolean {
                if(other !is Loaded) return false
                if(other.hasGrantedAccessibility != hasGrantedAccessibility) return false
                if(other.isLockScreenAvailable != isLockScreenAvailable) return false
                return other.widget.equalsForUi(widget)
            }
        }

        abstract fun equalsForUi(other: Any?): Boolean
    }

}

class WidgetConfigurationViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val appWidgetRepository: AppWidgetRepository,
    private val navigation: ConfigurationNavigation,
    private val rootNavigation: RootNavigation
): WidgetConfigurationViewModel() {

    private val appWidgetId = MutableStateFlow<Int?>(null)
    private val owner = MutableStateFlow<String?>(null)
    private val resumeBus = MutableStateFlow(System.currentTimeMillis())

    private val widget = appWidgetId.filterNotNull().flatMapLatest {
        databaseRepository.getAppWidgetById(it)
    }

    private val hasGrantedAccessibility = resumeBus.mapLatest {
        context.isServiceRunning(SmartspacerAccessibiltyService::class.java)
    }

    private val isLockScreenAvailable = combine(
        widget,
        owner.filterNotNull(),
        resumeBus
    ) { widget, owner, _ ->
        context.getDefaultLauncher() != (widget?.ownerPackage ?: owner)
    }

    override val state = combine(
        appWidgetId.filterNotNull(),
        widget,
        owner.filterNotNull(),
        hasGrantedAccessibility,
        isLockScreenAvailable
    ) { appWidgetId, widget, owner, hasGrantedAccessibility, isLockScreenAvailable ->
        State.Loaded(
            widget ?: AppWidget(appWidgetId, owner),
            hasGrantedAccessibility,
            isLockScreenAvailable
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun setup(appWidgetId: Int, owner: String) {
        viewModelScope.launch {
            this@WidgetConfigurationViewModelImpl.appWidgetId.emit(appWidgetId)
            this@WidgetConfigurationViewModelImpl.owner.emit(owner)
        }
    }

    override fun onResume() {
        viewModelScope.launch {
            resumeBus.emit(System.currentTimeMillis())
        }
    }

    override fun commitAndClose() {
        viewModelScope.launch {
            updateAppWidget { copy() }
            appWidgetRepository.trimWidgets()
            navigation.finish()
        }
    }

    override fun onAccessibilityClicked(context: Context) {
        viewModelScope.launch {
            //Kill the main app UI if it is running, so the user ends up back here
            rootNavigation.finish()
            navigation.navigate(Intent(context, AccessibilityPermissionActivity::class.java))
        }
    }

    override fun onTabChanged(index: Int) {
        updateAppWidget { copy(listMode = index == 1) }
    }

    override fun onSurfaceChanged(isLockscreen: Boolean) {
        updateAppWidget {
            copy(surface = if(isLockscreen) UiSurface.LOCKSCREEN else UiSurface.HOMESCREEN)
        }
    }

    override fun onSinglePageChanged(enabled: Boolean) {
        updateAppWidget {
            copy(multiPage = !enabled)
        }
    }

    override fun onShowControlsChanged(enabled: Boolean) {
        updateAppWidget {
            copy(showControls = enabled)
        }
    }

    override fun onInvisibleControlsChanged(enabled: Boolean) {
        updateAppWidget {
            copy(hideControls = enabled)
        }
    }

    override fun onAnimationChanged(enabled: Boolean) {
        updateAppWidget {
            copy(animate = enabled)
        }
    }

    override fun onTintColourChanged(tintColour: TintColour) {
        updateAppWidget {
            copy(tintColour = tintColour)
        }
    }

    override fun onShadowChanged(enabled: Boolean) {
        updateAppWidget {
            copy(showShadow = enabled)
        }
    }

    override fun onPaddingChanged(padding: Int) {
        updateAppWidget {
            copy(padding = padding)
        }
    }

    private fun updateAppWidget(block: AppWidget.() -> AppWidget) {
        val current = (state.value as? State.Loaded)?.widget ?: return
        viewModelScope.launch {
            databaseRepository.addAppWidget(block(current))
        }
    }

    override fun Context.getPagedWidget(
        appWidgetId: Int,
        session: PagedWidgetSmartspacerSessionState,
        config: AppWidget?
    ): RemoteViews? {
        return with(appWidgetRepository) {
            getPagedWidget(appWidgetId, session, config)
        }
    }

    override fun Context.getPageRemoteViews(
        appWidgetId: Int,
        view: SmartspaceView,
        config: AppWidget?,
        isList: Boolean,
        overflowIntent: Intent?,
        container: (() -> RemoteViews) -> RemoteViews
    ): RemoteViews {
        return with(appWidgetRepository) {
            getPageRemoteViews(appWidgetId, view, config, isList, overflowIntent, container)
        }
    }

    override fun Context.getListWidget(widget: AppWidget): RemoteViews {
        return with(appWidgetRepository) {
            getListWidget(widget)
        }
    }

}