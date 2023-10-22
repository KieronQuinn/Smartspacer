package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.getHeightSpan
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.getWidthSpan
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

abstract class ExpandedAddWidgetBottomSheetViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>
    abstract val addState: StateFlow<AddState>
    abstract val showSearchClear: StateFlow<Boolean>
    abstract val exitBus: Flow<Boolean>

    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String
    abstract fun onExpandClicked(item: Item.App)
    abstract fun onWidgetClicked(item: Item.Widget)

    abstract fun onWidgetBindResult(success: Boolean)
    abstract fun onWidgetConfigureResult(success: Boolean)
    abstract fun bindAppWidgetIfAllowed(provider: ComponentName, id: Int): Boolean
    abstract fun createConfigIntentSender(appWidgetId: Int): IntentSender

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>): State()
    }

    sealed class AddState {
        object Idle: AddState()
        data class BindWidget(
            val info: AppWidgetProviderInfo,
            val id: Int
        ): AddState()
        data class ConfigureWidget(
            val info: AppWidgetProviderInfo,
            val id: Int
        ): AddState()
        object Dismiss: AddState()
        object WidgetError: AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class Item(val type: Type) {
        object Header: Item(Type.HEADER)

        data class App(
            val packageName: String,
            val label: CharSequence,
            val isExpanded: Boolean = false,
            val count: Int = 0
        ): Item(Type.APP)

        data class Widget(
            val label: CharSequence,
            val info: AppWidgetProviderInfo,
            val spanX: Int,
            val spanY: Int
        ): Item(Type.WIDGET)

        enum class Type {
            HEADER, APP, WIDGET
        }
    }

}

class ExpandedAddWidgetBottomSheetViewModelImpl(
    context: Context,
    widgetRepository: WidgetRepository,
    private val expandedRepository: ExpandedRepository,
    private val databaseRepository: DatabaseRepository,
    scope: CoroutineScope? = null
): ExpandedAddWidgetBottomSheetViewModel(scope) {
    private val packageManager = context.packageManager
    private val expandedPackages = MutableStateFlow<Set<String>>(emptySet())

    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val addState = MutableStateFlow<AddState>(AddState.Idle)

    override val showSearchClear = searchTerm.mapLatest {
        it.isNotBlank()
    }.stateIn(vmScope, SharingStarted.Eagerly, false)

    private val providers = widgetRepository.providers.mapLatest {
        it.groupBy { provider -> provider.provider.packageName }.mapNotNull { app ->
            //Exclude own widgets
            if(app.key == BuildConfig.APPLICATION_ID) return@mapNotNull null
            val label = packageManager.getPackageLabel(app.key) ?: return@mapNotNull null
            val widgets = app.value.map { widget ->
                val spanX = widget.getWidthSpan()
                val spanY = widget.getHeightSpan()
                Item.Widget(widget.loadLabel(packageManager), widget, spanX, spanY)
            }.sortedBy { widget -> widget.label.toString().lowercase() }
            Pair(Item.App(app.key, label), widgets)
        }.toMap()
    }.flowOn(Dispatchers.IO)

    override val state = combine(
        providers,
        searchTerm,
        expandedPackages
    ) { providers, term, expanded ->
        val widgets = providers.mapNotNull {
            if(it.key.label.contains(term, true)) {
                Pair(it.key, it.value)
            }
            val widgets = it.value.filter { widget ->
                widget.label.contains(term, true)
            }
            if(widgets.isEmpty()){
                return@mapNotNull null
            }
            Pair(it.key, widgets)
        }.sortedBy { it.first.label.toString().lowercase() }.map {
            val isExpanded = expanded.contains(it.first.packageName)
            val expandedWidgets = if(isExpanded){
                it.second.toTypedArray()
            }else{
                emptyArray()
            }
            val app = it.first.copy(isExpanded = isExpanded, count = it.second.size)
            listOf(app, *expandedWidgets)
        }.flatten()
        State.Loaded(listOf(Item.Header) + widgets)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override val exitBus = context.lockscreenShowing().drop(1)

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun onExpandClicked(item: Item.App) {
        vmScope.launch {
            val expanded = expandedPackages.value
            val newExpanded = if (expanded.contains(item.packageName)) {
                expanded.minus(item.packageName)
            }else{
                expanded.plus(item.packageName)
            }
            expandedPackages.emit(newExpanded)
        }
    }

    override fun onWidgetClicked(item: Item.Widget) {
        vmScope.launch {
            val id = allocateAppWidgetId()
            addState.emit(AddState.BindWidget(item.info, id))
        }
    }

    override fun onWidgetBindResult(success: Boolean) {
        vmScope.launch {
            val current = (addState.value as? AddState.BindWidget) ?: return@launch
            when {
                !success -> {
                    deallocateAppWidgetId(current.id)
                }
                current.info.configure != null -> {
                    addState.emit(AddState.ConfigureWidget(current.info, current.id))
                }
                else -> {
                    addWidgetToDatabase(current.info, current.id)
                    addState.emit(AddState.Dismiss)
                }
            }
        }
    }

    override fun onWidgetConfigureResult(success: Boolean) {
        vmScope.launch {
            val current = (addState.value as? AddState.ConfigureWidget) ?: return@launch
            when {
                !success -> {
                    deallocateAppWidgetId(current.id)
                }
                else -> {
                    addWidgetToDatabase(current.info, current.id)
                    addState.emit(AddState.Dismiss)
                }
            }
        }
    }

    private suspend fun addWidgetToDatabase(
        info: AppWidgetProviderInfo,
        id: Int
    ) = withContext(Dispatchers.IO) {
        val nextId = databaseRepository.getExpandedCustomAppWidgets().first().maxOfOrNull {
            it.index
        } ?: 0
        val widget = ExpandedCustomAppWidget(
            appWidgetId = id,
            provider = info.provider.flattenToString(),
            index = nextId,
            spanX = info.getWidthSpan(),
            spanY = info.getHeightSpan(),
            showWhenLocked = true
        )
        databaseRepository.addExpandedCustomAppWidget(widget)
    }

    private fun allocateAppWidgetId(): Int {
        return expandedRepository.allocateAppWidgetId()
    }

    private fun deallocateAppWidgetId(id: Int) {
        expandedRepository.deallocateAppWidgetId(id)
    }

    override fun bindAppWidgetIfAllowed(provider: ComponentName, id: Int): Boolean {
        return expandedRepository.bindAppWidgetIdIfAllowed(id, provider)
    }

    override fun createConfigIntentSender(appWidgetId: Int): IntentSender {
        return expandedRepository.createConfigIntentSender(appWidgetId)
    }

}