package com.kieronquinn.app.smartspacer.ui.screens.expanded.options

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.allowBackground
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class ExpandedWidgetOptionsBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val exitBus: Flow<Boolean>

    abstract fun setup(appWidgetId: Int, canReconfigure: Boolean)
    abstract fun setSpanX(spanX: Float)
    abstract fun setSpanY(spanY: Float)
    abstract fun setShowWhenLocked(showWhenLocked: Boolean)
    abstract fun onFullWidthChanged(enabled: Boolean)
    abstract fun onRoundCornersChanged(enabled: Boolean)
    abstract fun onReconfigureClicked(configureLauncher: ActivityResultLauncher<IntentSenderRequest>)

    sealed class State {
        data object Loading: State()
        data class Loaded(
            val spanX: Int,
            val spanY: Int,
            val showWhenLocked: Boolean,
            val canReconfigure: Boolean,
            val roundCorners: Boolean,
            val fillWidth: Boolean
        ): State()
    }

}

class ExpandedWidgetOptionsBottomSheetViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val expandedRepository: ExpandedRepository,
    scope: CoroutineScope? = null
): ExpandedWidgetOptionsBottomSheetViewModel(scope) {

    private val appWidgetId = MutableStateFlow<Int?>(null)
    private val canReconfigure = MutableStateFlow<Boolean?>(null)

    @VisibleForTesting
    val widget = combine(
        appWidgetId.filterNotNull(),
        expandedRepository.expandedCustomAppWidgets
    ) { id, widgets ->
        widgets.firstOrNull { it.appWidgetId == id }
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override val state = combine(
        widget.filterNotNull(),
        canReconfigure.filterNotNull()
    ) { widget, reconfigure ->
        State.Loaded(
            widget.spanX,
            widget.spanY,
            widget.showWhenLocked,
            reconfigure,
            widget.roundCorners,
            widget.fullWidth
        )
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val exitBus = context.lockscreenShowing()
        .drop(1)

    override fun setup(appWidgetId: Int, canReconfigure: Boolean) {
        vmScope.launch {
            this@ExpandedWidgetOptionsBottomSheetViewModelImpl.appWidgetId.emit(appWidgetId)
            this@ExpandedWidgetOptionsBottomSheetViewModelImpl.canReconfigure.emit(canReconfigure)
        }
    }

    override fun setSpanX(spanX: Float) {
        val widget = widget.value ?: return
        vmScope.launch {
            databaseRepository.updateExpandedCustomAppWidget(widget.copy(spanX = spanX.toInt()))
        }
    }

    override fun setSpanY(spanY: Float) {
        val widget = widget.value ?: return
        vmScope.launch {
            databaseRepository.updateExpandedCustomAppWidget(widget.copy(spanY = spanY.toInt()))
        }
    }

    override fun setShowWhenLocked(showWhenLocked: Boolean) {
        val widget = widget.value ?: return
        vmScope.launch {
            databaseRepository.updateExpandedCustomAppWidget(
                widget.copy(showWhenLocked = showWhenLocked)
            )
        }
    }

    override fun onFullWidthChanged(enabled: Boolean) {
        val widget = widget.value ?: return
        vmScope.launch {
            databaseRepository.updateExpandedCustomAppWidget(
                widget.copy(fullWidth = enabled)
            )
        }
    }

    override fun onRoundCornersChanged(enabled: Boolean) {
        val widget = widget.value ?: return
        vmScope.launch {
            databaseRepository.updateExpandedCustomAppWidget(
                widget.copy(roundCorners = enabled)
            )
        }
    }

    override fun onReconfigureClicked(configureLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val appWidgetId = appWidgetId.value ?: return
        expandedRepository.createConfigIntentSender(appWidgetId).also {
            configureLauncher.launch(
                IntentSenderRequest.Builder(it).build(),
                ActivityOptionsCompat.makeBasic().allowBackground()
            )
        }
    }

}