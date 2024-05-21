package com.kieronquinn.app.smartspacer.ui.screens.expanded.rearrange

import android.content.Context
import android.os.Process
import com.kieronquinn.app.smartspacer.components.navigation.ExpandedNavigation
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

abstract class ExpandedRearrangeViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val exitBus: Flow<Boolean>

    abstract fun onBackPressed()
    abstract fun onPause()
    abstract fun onResume()
    abstract fun moveItem(from: Int, to: Int)

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>, val multiColumnEnabled: Boolean): State()
    }

}

class ExpandedRearrangeViewModelImpl(
    context: Context,
    private val navigation: ExpandedNavigation,
    private val databaseRepository: DatabaseRepository,
    expandedRepository: ExpandedRepository,
    settingsRepository: SmartspacerSettingsRepository,
    scope: CoroutineScope? = null
): ExpandedRearrangeViewModel(scope) {

    private val widgets = expandedRepository.expandedCustomAppWidgets
        .stateIn(vmScope, SharingStarted.Eagerly, null)

    private val items = MutableSharedFlow<List<Item>>()

    private val session = ExpandedSmartspacerSession(
        context,
        SmartspaceSessionId(UUID.randomUUID().toString(), Process.myUserHandle()),
        ::onItemsChanged
    ).also {
        //Set dummy values for unused other items on this screen
        it.setTopInset(0)
    }

    override val state = combine(
        items,
        settingsRepository.expandedMultiColumnEnabled.asFlow()
    ) { items, multiColumn ->
        val widgets = items.filter { item ->
            (item is Item.Widget && item.isCustom) || item is Item.RemovedWidget
        }
        State.Loaded(widgets, multiColumn)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val exitBus = context.lockscreenShowing()
        .drop(1)

    override fun onBackPressed() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun moveItem(from: Int, to: Int) {
        vmScope.launch {
            val items = widgets.firstNotNull()
            if (from < to) {
                for (i in from until to) {
                    items.swap(i, i + 1)
                }
            } else {
                for (i in from downTo to + 1) {
                    items.swap(i, i - 1)
                }
            }
            items.forEachIndexed { index, item ->
                item.index = index
                databaseRepository.updateExpandedCustomAppWidget(item)
            }
        }
    }

    private fun <I> List<I>.swap(from: Int, to: Int) {
        try {
            Collections.swap(this, from, to)
        }catch (e: IndexOutOfBoundsException){
            //Concurrent modification?
        }
    }

    override fun onPause() {
        session.onPause()
    }

    override fun onResume() {
        session.onResume()
    }

    override fun onCleared() {
        session.onDestroy()
        super.onCleared()
    }

    private fun onItemsChanged(items: List<Item>) {
        vmScope.launch {
            this@ExpandedRearrangeViewModelImpl.items.emit(items)
        }
    }

}