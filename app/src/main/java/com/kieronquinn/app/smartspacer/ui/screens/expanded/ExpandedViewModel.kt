package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcut
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState.Shortcuts.Shortcut
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

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
    abstract fun onOptionsClicked(appWidgetId: Int)
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

    sealed class State {
        data object Loading: State()
        data object Disabled: State()
        data object PermissionRequired: State()
        data class Loaded(
            val isLocked: Boolean,
            val lightStatusIcons: Boolean,
            val items: List<Item>
        ): State()
    }

    /**
     *  Shows a grid of just Complications, instead of padding with date targets (which often
     *  leads to duplication)
     */
    data class Complications(
        val complications: List<Complication>
    ) {

        sealed class Complication(open val parent: SmartspaceTarget, val type: Type) {

            data class Action(
                override val parent: SmartspaceTarget,
                val smartspaceAction: SmartspaceAction
            ): Complication(parent, Type.ACTION) {

                override fun equals(other: Any?): Boolean {
                    if(other !is Action) return false
                    return smartspaceAction == other.smartspaceAction
                }

                override fun hashCode(): Int {
                    var result = parent.hashCode()
                    result = 31 * result + smartspaceAction.hashCode()
                    return result
                }

                override fun isValid(): Boolean {
                    return smartspaceAction.icon != null || smartspaceAction.title.isNotBlank()
                }

            }

            data class SubItemInfo(
                override val parent: SmartspaceTarget,
                val info: BaseTemplateData.SubItemInfo
            ): Complication(parent, Type.SUB_ITEM_INFO) {

                override fun equals(other: Any?): Boolean {
                    if(other !is SubItemInfo) return false
                    return info == other.info
                }

                override fun hashCode(): Int {
                    var result = parent.hashCode()
                    result = 31 * result + info.hashCode()
                    return result
                }

                override fun isValid(): Boolean {
                    return info.icon != null || info.text?.text?.isNotBlank() == true
                }

            }

            abstract fun isValid(): Boolean

            enum class Type {
                ACTION,
                SUB_ITEM_INFO
            }
        }

    }

}

class ExpandedViewModelImpl(
    context: Context,
    private val expandedRepository: ExpandedRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    private val navigation: ExpandedNavigation
): ExpandedViewModel() {

    private val isLocked = context.lockscreenShowing()
    private val items = MutableSharedFlow<List<Item>>()
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private var widgetBindState: WidgetBindState? = null
    private val isOverlay = MutableStateFlow<Boolean?>(null)
    private val isResumed = MutableStateFlow(false)

    private val session = ExpandedSmartspacerSession(
        context,
        SmartspaceSessionId(UUID.randomUUID().toString(), Process.myUserHandle()),
        ::onItemsChanged
    )

    private suspend fun onItemsChanged(items: List<Item>) {
        this@ExpandedViewModelImpl.items.emit(items)
    }

    private val hasDisplayOverOtherAppsPermission = combine(
        isOverlay.filterNotNull(),
        isResumed
    ) { overlay, resumed ->
        //If not overlay or the overlay hasn't opened yet, permission isn't required
        if(!overlay || !resumed) return@combine true
        Settings.canDrawOverlays(context)
    }

    override val state = combine(
        settingsRepository.expandedModeEnabled.asFlow(),
        isLocked,
        items,
        hasDisplayOverOtherAppsPermission
    ) { expanded, locked, list, permission ->
        when {
            !permission && expanded -> {
                State.PermissionRequired
            }
            expanded -> {
                val search = list.filterIsInstance<Item.Search>().firstOrNull()
                val isLightStatusBar = search?.isLightStatusBar ?: false
                State.Loaded(locked, isLightStatusBar, list)
            }
            else -> {
                State.Disabled
            }
        }
    }.flowOn(Dispatchers.IO).onEach {
        if(it is State.PermissionRequired) {
            settingsRepository.requiresDisplayOverOtherAppsPermission.set(true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override val exitBus = isLocked.drop(1).mapLatest {
        it && settingsRepository.expandedCloseWhenLocked.get()
    }.filterNotNull()

    override val overlayDrag = expandedRepository.overlayDragProgressChanged.map {}

    override fun setup(isOverlay: Boolean) {
        viewModelScope.launch {
            this@ExpandedViewModelImpl.isOverlay.emit(isOverlay)
        }
    }

    override fun onPause() {
        session.onPause()
        viewModelScope.launch {
            isResumed.emit(false)
        }
    }

    override fun onResume() {
        session.onResume()
        viewModelScope.launch {
            isResumed.emit(true)
        }
    }

    override fun setTopInset(inset: Int) {
        session.setTopInset(inset)
    }

    override fun onTargetInteraction(target: SmartspaceTarget, actionId: String?) {
        session.notifySmartspaceEvent(
            SmartspaceTargetEvent(target, actionId, SmartspaceTargetEvent.EVENT_TARGET_INTERACTION)
        )
    }

    override fun onTargetDismiss(target: SmartspaceTarget) {
        session.notifySmartspaceEvent(
            SmartspaceTargetEvent(
                target, null, SmartspaceTargetEvent.EVENT_TARGET_DISMISS
            )
        )
    }

    override fun onDeleteCustomWidget(appWidgetId: Int) {
        session.onDeleteCustomWidget(appWidgetId)
    }

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

    override fun onOptionsClicked(appWidgetId: Int) {
        viewModelScope.launch {
            navigation.navigate(ExpandedFragmentDirections.actionExpandedFragmentToExpandedWidgetOptionsBottomSheetFragment(appWidgetId))
        }
    }

    override fun onRearrangeClicked() {
        viewModelScope.launch {
            navigation.navigate(ExpandedFragmentDirections.actionExpandedFragmentToExpandedRearrangeFragment())
        }
    }

    override fun onCleared() {
        session.onDestroy()
        super.onCleared()
    }

    data class WidgetBindState(
        val appWidgetId: Int,
        val info: AppWidgetProviderInfo,
        val id: String?,
        val config: CustomExpandedAppWidgetConfig?
    )

}