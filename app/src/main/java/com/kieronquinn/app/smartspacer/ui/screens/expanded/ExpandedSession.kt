package com.kieronquinn.app.smartspacer.ui.screens.expanded

import android.content.Context
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession.State
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

interface ExpandedSession {

    val state: StateFlow<State>
    val exitBus: Flow<Boolean>

    fun setup(isOverlay: Boolean)
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun setTopInset(inset: Int)
    fun onTargetInteraction(target: SmartspaceTarget, actionId: String?)
    fun onTargetDismiss(target: SmartspaceTarget)
    fun onDeleteCustomWidget(appWidgetId: Int)

    sealed class State {
        data object Loading: State()
        data object Disabled: State()
        data object PermissionRequired: State()
        data class Loaded(
            val isLocked: Boolean,
            val lightStatusIcons: Boolean,
            val multiColumnEnabled: Boolean,
            val items: List<ExpandedSmartspacerSession.Item>
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

class ExpandedSessionImpl(
    context: Context,
    private val settingsRepository: SmartspacerSettingsRepository,
): ExpandedSession {

    private val scope = MainScope()
    private val isOverlay = MutableStateFlow<Boolean?>(null)
    private val isLocked = context.lockscreenShowing()
    private val items = MutableSharedFlow<List<ExpandedSmartspacerSession.Item>>()
    private val isResumed = MutableStateFlow(false)

    private suspend fun onItemsChanged(items: List<ExpandedSmartspacerSession.Item>) {
        this.items.emit(items)
    }

    private val session = ExpandedSmartspacerSession(
        context,
        SmartspaceSessionId(UUID.randomUUID().toString(), Process.myUserHandle()),
        ::onItemsChanged
    )

    override val exitBus = isLocked.drop(1).mapLatest {
        it && settingsRepository.expandedCloseWhenLocked.get()
    }.filterNotNull()

    private val hideAddButton = combine(
        isOverlay.filterNotNull(),
        settingsRepository.expandedHideAddButton.asFlow()
    ) { overlay, hideAdd ->
        when(hideAdd) {
            SmartspacerSettingsRepository.ExpandedHideAddButton.NEVER -> false
            SmartspacerSettingsRepository.ExpandedHideAddButton.ALWAYS -> true
            SmartspacerSettingsRepository.ExpandedHideAddButton.OVERLAY_ONLY -> overlay
        }
    }

    private val filteredItems = combine(
        items,
        hideAddButton
    ) { items, hideAdd ->
        items.filterNot { it is ExpandedSmartspacerSession.Item.Footer && hideAdd }
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
        filteredItems,
        hasDisplayOverOtherAppsPermission,
        settingsRepository.expandedMultiColumnEnabled.asFlow()
    ) { expanded, locked, list, permission, multiColumn ->
        when {
            !permission && expanded -> {
                State.PermissionRequired
            }
            expanded -> {
                val search = list.filterIsInstance<ExpandedSmartspacerSession.Item.Search>().firstOrNull()
                val isLightStatusBar = search?.isLightStatusBar ?: false
                State.Loaded(locked, isLightStatusBar, multiColumn, list)
            }
            else -> {
                State.Disabled
            }
        }
    }.flowOn(Dispatchers.IO).onEach {
        if(it is State.PermissionRequired) {
            settingsRepository.requiresDisplayOverOtherAppsPermission.set(true)
        }
    }.stateIn(scope, SharingStarted.Eagerly, State.Loading)

    override fun setup(isOverlay: Boolean) {
        scope.launch {
            this@ExpandedSessionImpl.isOverlay.emit(isOverlay)
        }
    }

    override fun onPause() {
        session.onPause()
        scope.launch {
            isResumed.emit(false)
        }
    }

    override fun onResume() {
        session.onResume()
        scope.launch {
            isResumed.emit(true)
        }
    }

    override fun onDestroy() {
        session.onDestroy()
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

}