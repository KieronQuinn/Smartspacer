package com.kieronquinn.app.smartspacer.ui.screens.expanded.addwidget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import android.graphics.drawable.Icon
import android.os.Build
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.WidgetCategory
import com.kieronquinn.app.smartspacer.utils.extensions.getCategory
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.widget.WidgetMapping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    abstract fun onWidgetClicked(item: Item.Widget, spanX: Int, spanY: Int)

    abstract fun onWidgetBindResult(success: Boolean)
    abstract fun onWidgetConfigureResult(success: Boolean)
    abstract fun bindAppWidgetIfAllowed(provider: ComponentName, id: Int): Boolean
    abstract fun createConfigIntentSender(appWidgetId: Int): IntentSender

    sealed class State {
        data object Loading: State()
        data class Loaded(val items: List<Item>): State()
    }

    sealed class AddState {
        data object Idle: AddState()
        data class BindWidget(
            val info: AppWidgetProviderInfo,
            val spanX: Int,
            val spanY: Int,
            val id: Int
        ): AddState()
        data class ConfigureWidget(
            val info: AppWidgetProviderInfo,
            val spanX: Int,
            val spanY: Int,
            val id: Int
        ): AddState()
        data object Dismiss: AddState()
        object WidgetError: AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class Item(val type: Type) {
        data class Predicted(
            val widgets: List<Widget>
        ): Item(Type.PREDICTED)

        data class App(
            val identifier: String,
            val packageName: String,
            val label: CharSequence,
            val icon: Icon?,
            val isExpanded: Boolean = false,
            val count: Int = 0
        ): Item(Type.APP)

        data class Widget(
            val parent: App,
            val category: WidgetCategory,
            val label: CharSequence,
            val description: CharSequence?,
            val info: AppWidgetProviderInfo
        ): Item(Type.WIDGET)

        enum class Type {
            PREDICTED, APP, WIDGET
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
        it.mapNotNull { widget ->
            val packageName = widget.provider.packageName
            //Exclude own widgets
            if(packageName == BuildConfig.APPLICATION_ID) return@mapNotNull null
            val label = packageManager.getPackageLabel(packageName) ?: return@mapNotNull null
            val mapping = WidgetMapping.getWidgetMapping(widget.provider)
            val app = if(mapping != null) {
                Item.App(
                    mapping.identifier,
                    packageName,
                    context.getString(mapping.label),
                    Icon.createWithResource(context, mapping.icon)
                )
            }else{
                Item.App(
                    packageName,
                    packageName,
                    label,
                    null
                )
            }
            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                widget.loadDescription(context)
            } else null
            Item.Widget(
                app,
                widget.getCategory(),
                widget.loadLabel(packageManager),
                description,
                widget
            )
        }.groupBy { widget ->
            widget.parent.identifier
        }.map { item ->
            val app = item.value.first().parent
            val widget = item.value.sortedBy { widget -> widget.label.toString().lowercase() }
            Pair(app, widget)
        }.toMap()
    }.flowOn(Dispatchers.IO)

    private val predictedWidgets = flow {
        emit(expandedRepository.getPredictedWidgets())
    }.flowOn(Dispatchers.IO)

    private val predicted = combine(
        predictedWidgets,
        providers
    ) { predictedWidgets, providers ->
        val allWidgets = providers.map { it.value }.flatten()
        predictedWidgets
            //Ignore if we got no predictions
            .takeIf { it.isNotEmpty() }
            //Find the already loaded widget info for this provider
            ?.mapNotNull { allWidgets.firstOrNull { widget -> widget.info == it } }
            //Group all of the predicted widgets into their categories to sort them
            ?.groupBy { it.category }
            //Take at most one of each category
            ?.mapValues { it.value.take(1) }
            //Remove category splitting and flatten back into one list
            ?.flatMap { it.value }
            //Categories are sorted in the same way they are in the Pixel Launcher
            ?.sortedBy { it.category.ordinal }
            ?.let { Item.Predicted(it) }
    }

    override val state = combine(
        providers,
        predicted,
        searchTerm,
        expandedPackages
    ) { providers, predicted, term, expanded ->
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
            val isExpanded = expanded.contains(it.first.identifier)
            val expandedWidgets = if(isExpanded){
                it.second.toTypedArray()
            }else{
                emptyArray()
            }
            val app = it.first.copy(isExpanded = isExpanded, count = it.second.size)
            listOf(app, *expandedWidgets)
        }.flatten()
        State.Loaded(listOfNotNull(predicted) + widgets)
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
            val newExpanded = if (expanded.contains(item.identifier)) {
                expanded.minus(item.identifier)
            }else{
                expanded.plus(item.identifier)
            }
            expandedPackages.emit(newExpanded)
        }
    }

    override fun onWidgetClicked(item: Item.Widget, spanX: Int, spanY: Int) {
        vmScope.launch {
            val id = allocateAppWidgetId()
            addState.emit(AddState.BindWidget(item.info, spanX, spanY, id))
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
                    addState.emit(
                        AddState.ConfigureWidget(
                            current.info,
                            current.spanX,
                            current.spanY,
                            current.id
                        )
                    )
                }
                else -> {
                    addWidgetToDatabase(
                        current.info,
                        current.spanX,
                        current.spanY,
                        current.id
                    )
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
                    addWidgetToDatabase(current.info, current.spanX, current.spanY, current.id)
                    addState.emit(AddState.Dismiss)
                }
            }
        }
    }

    private suspend fun addWidgetToDatabase(
        info: AppWidgetProviderInfo,
        spanX: Int,
        spanY: Int,
        id: Int
    ) = withContext(Dispatchers.IO) {
        val nextId = databaseRepository.getExpandedCustomAppWidgets().first().maxOfOrNull {
            it.index
        } ?: 0
        val widget = ExpandedCustomAppWidget(
            appWidgetId = id,
            provider = info.provider.flattenToString(),
            index = nextId,
            spanX = spanX,
            spanY = spanY,
            showWhenLocked = true,
            roundCorners = true,
            fullWidth = false
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