package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigation
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.State
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class ExpandedViewModel: ViewModel() {

    abstract val state: StateFlow<State>
    abstract val overlayDrag: Flow<Unit>
    abstract val exitBus: Flow<Boolean>

    abstract fun setup(isOverlay: Boolean)
    abstract fun onResume()
    abstract fun onPause()
    abstract fun setTopInset(inset: Int)
    abstract fun onTargetInteraction(target: SmartspaceTarget, actionId: String?)
    abstract fun onTargetDismiss(target: SmartspaceTarget)
    abstract fun onDeleteCustomWidget(appWidgetId: Int)

    abstract fun onShortcutClicked(context: Context, shortcut: Shortcut)
    abstract fun onAppShortcutClicked(appShortcut: AppShortcut)
    abstract fun onAddWidgetClicked()
    abstract fun onAppWidgetReset(appWidgetId: Int)
    abstract fun onOptionsClicked(appWidgetId: Int, canReconfigure: Boolean)
    abstract fun onRearrangeClicked()

    abstract fun onConfigureWidgetClicked(
        bindLauncher: ActivityResultLauncher<Intent>,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        info: AppWidgetProviderInfo,
        id: String?,
        config: CustomExpandedAppWidgetConfig?
    )

    abstract fun onWidgetBindResult(
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        success: Boolean
    )

    abstract fun onWidgetConfigureResult(success: Boolean)

}

class ExpandedViewModelImpl(
    private val sessionId: String,
    context: Context,
    private val expandedRepository: ExpandedRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    private val navigation: ExpandedNavigation
): ExpandedViewModel() {

    private val session = expandedRepository.getExpandedSession(context, sessionId)

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private var widgetBindState: WidgetBindState? = null

    override val overlayDrag = expandedRepository.overlayDragProgressChanged.map {}
    override val state = session.state
    override val exitBus = session.exitBus

    override fun setup(isOverlay: Boolean) = session.setup(isOverlay)

    override fun onResume() = session.onResume()

    override fun onPause() = session.onPause()

    override fun setTopInset(inset: Int) = session.setTopInset(inset)

    override fun onTargetInteraction(target: SmartspaceTarget, actionId: String?) =
        session.onTargetInteraction(target, actionId)

    override fun onTargetDismiss(target: SmartspaceTarget) = session.onTargetDismiss(target)

    override fun onDeleteCustomWidget(appWidgetId: Int) = session.onDeleteCustomWidget(appWidgetId)

    override fun onConfigureWidgetClicked(
        bindLauncher: ActivityResultLauncher<Intent>,
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        info: AppWidgetProviderInfo,
        id: String?,
        config: CustomExpandedAppWidgetConfig?
    ) {
        widgetBindState = null
        val appWidgetId = expandedRepository.allocateAppWidgetId()
        when {
            !appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider) -> {
                widgetBindState = WidgetBindState(appWidgetId, info, id, config)
                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                }
                bindLauncher.launch(bindIntent)
            }
            info.configure != null -> {
                widgetBindState = WidgetBindState(appWidgetId, info, id, config)
                expandedRepository.createConfigIntentSender(appWidgetId).also {
                    configureLauncher.launch(
                        IntentSenderRequest.Builder(it).build(),
                        ActivityOptionsCompat.makeBasic().allowBackground()
                    )
                }
            }
            else -> {
                onAppWidgetAdded(appWidgetId, info, id, config)
            }
        }
    }

    override fun onWidgetBindResult(
        configureLauncher: ActivityResultLauncher<IntentSenderRequest>,
        success: Boolean
    ) {
        val state = widgetBindState ?: return
        widgetBindState = null
        when {
            success && state.info.configure != null -> {
                expandedRepository.createConfigIntentSender(state.appWidgetId).also {
                    configureLauncher.launch(
                        IntentSenderRequest.Builder(it).build(),
                        ActivityOptionsCompat.makeBasic().allowBackground()
                    )
                }
            }
            success -> {
                onAppWidgetAdded(state.appWidgetId, state.info, state.id, state.config)
            }
            else -> {
                onAppWidgetFailed(state.appWidgetId)
            }
        }
    }

    override fun onWidgetConfigureResult(success: Boolean) {
        val state = widgetBindState ?: return
        widgetBindState = null
        if(success){
            onAppWidgetAdded(state.appWidgetId, state.info, state.id, state.config)
        }else{
            onAppWidgetFailed(state.appWidgetId)
        }
    }

    private fun onAppWidgetAdded(
        appWidgetId: Int,
        info: AppWidgetProviderInfo,
        id: String?,
        config: CustomExpandedAppWidgetConfig?
    ) {
        viewModelScope.launch {
            expandedRepository.commitExpandedAppWidget(info, appWidgetId, id, config)
        }
    }

    private fun onAppWidgetFailed(appWidgetId: Int) {
        expandedRepository.deallocateAppWidgetId(appWidgetId)
    }

    override fun onAppShortcutClicked(appShortcut: AppShortcut) {
        viewModelScope.launch {
            shizukuServiceRepository.runWithService {
                it.startShortcut(appShortcut.packageName, appShortcut.shortcutId)
            }
        }
    }

    override fun onShortcutClicked(context: Context, shortcut: Shortcut) {
        when {
            shortcut.pendingIntent != null -> {
                shortcut.pendingIntent?.sendSafely()
            }
            shortcut.intent != null -> {
                try {
                    context.startActivity(shortcut.intent)
                }catch (e: ActivityNotFoundException){
                    //No-op
                }
            }
        }
    }

    override fun onAddWidgetClicked() {
        viewModelScope.launch {
            settingsRepository.expandedHasClickedAdd.set(true)
            navigation.navigate(ExpandedFragmentDirections.actionExpandedFragmentToExpandedAddWidgetBottomSheetFragment())
        }
    }

    override fun onAppWidgetReset(appWidgetId: Int) {
        viewModelScope.launch {
            expandedRepository.removeAppWidget(appWidgetId)
        }
    }

    override fun onOptionsClicked(appWidgetId: Int, canReconfigure: Boolean) {
        viewModelScope.launch {
            navigation.navigate(ExpandedFragmentDirections.actionExpandedFragmentToExpandedWidgetOptionsBottomSheetFragment(appWidgetId, canReconfigure))
        }
    }

    override fun onRearrangeClicked() {
        viewModelScope.launch {
            navigation.navigate(ExpandedFragmentDirections.actionExpandedFragmentToExpandedRearrangeFragment())
        }
    }

    override fun onCleared() {
        expandedRepository.destroyExpandedSession(sessionId)
        super.onCleared()
    }

    data class WidgetBindState(
        val appWidgetId: Int,
        val info: AppWidgetProviderInfo,
        val id: String?,
        val config: CustomExpandedAppWidgetConfig?
    )

}