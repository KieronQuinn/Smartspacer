package com.kieronquinn.app.smartspacer.ui.screens.expanded.options

import android.content.Context
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

abstract class ExpandedWidgetOptionsBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val exitBus: Flow<Boolean>

    abstract fun setupWithWidgetId(appWidgetId: Int)
    abstract fun setSpanX(spanX: Float)
    abstract fun setSpanY(spanY: Float)
    abstract fun setShowWhenLocked(showWhenLocked: Boolean)

    sealed class State {
        object Loading: State()
        data class Loaded(
            val spanX: Int,
            val spanY: Int,
            val showWhenLocked: Boolean
        ): State()
    }

}

class ExpandedWidgetOptionsBottomSheetViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    expandedRepository: ExpandedRepository,
    scope: CoroutineScope? = null
): ExpandedWidgetOptionsBottomSheetViewModel(scope) {

    private val appWidgetId = MutableStateFlow<Int?>(null)

    @VisibleForTesting
    val widget = combine(
        appWidgetId.filterNotNull(),
        expandedRepository.expandedCustomAppWidgets
    ) { id, widgets ->
        widgets.first { it.appWidgetId == id }
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    override val state = widget.filterNotNull().mapLatest {
        State.Loaded(it.spanX, it.spanY, it.showWhenLocked)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val exitBus = context.lockscreenShowing().drop(1)

    override fun setupWithWidgetId(appWidgetId: Int) {
        vmScope.launch {
            this@ExpandedWidgetOptionsBottomSheetViewModelImpl.appWidgetId.emit(appWidgetId)
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

}