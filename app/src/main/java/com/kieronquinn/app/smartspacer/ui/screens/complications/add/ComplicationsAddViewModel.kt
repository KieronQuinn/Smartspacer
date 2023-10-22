package com.kieronquinn.app.smartspacer.ui.screens.complications.add

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

abstract class ComplicationsAddViewModel(
    context: Context,
    targetsRepository: TargetsRepository,
    databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseAddComplicationsViewModelImpl(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    abstract val state: StateFlow<State>
    abstract val showSearchClear: StateFlow<Boolean>

    abstract fun setSearchTerm(term: String)
    abstract fun getSearchTerm(): String
    abstract fun onExpandClicked(item: Item.App)

    abstract fun dismiss()

}

class ComplicationsAddViewModelImpl(
    context: Context,
    private val targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    private val navigation: ContainerNavigation,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
): ComplicationsAddViewModel(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    private val packageManager = context.packageManager
    private val expandBus = MutableStateFlow(System.currentTimeMillis())
    
    @VisibleForTesting
    val searchTerm = MutableStateFlow("")

    override val showSearchClear = searchTerm.mapLatest { it.isNotEmpty() }
        .stateIn(vmScope, SharingStarted.Eagerly, searchTerm.value.isNotEmpty())

    private val complications = flow {
        val existingComplications = databaseRepository.getActions().first().map { it.authority }
        val newComplications = targetsRepository.getAllComplications().mapNotNull {
            val config = it.getPluginConfig().firstNotNull()
            if(existingComplications.contains(it.authority) && !config.allowAddingMoreThanOnce){
                return@mapNotNull null
            }
            Item.Complication(
                it.sourcePackage,
                it.authority,
                UUID.randomUUID().toString(),
                config.label,
                config.description,
                config.icon,
                config.compatibilityState,
                config.setupActivity,
                config.widgetProvider,
                config.notificationProvider,
                config.broadcastProvider
            )
        }.sortedBy {
            it.label.toString().lowercase()
        }
        val groupedComplications = newComplications.groupBy { it.packageName }.mapKeys {
            val label = packageManager.getPackageLabel(it.key) ?: ""
            Item.App(it.key, label)
        }.toList().sortedBy { it.first.label.toString().lowercase() }
        emit(groupedComplications)
    }.flowOn(Dispatchers.IO)

    override val state = combine(complications, searchTerm, expandBus) { t, s, _ ->
        val complicationList = ArrayList<Item>()
        val searchTerm = s.trim()
        t.forEach {
            //If the app's name matches the term, return all its complications
            if(it.first.label.contains(searchTerm, true)) {
                complicationList.add(it.first)
                if(it.first.isExpanded) {
                    it.second.forEachIndexed { index, complication ->
                        complicationList.add(
                            complication.copy(isLastComplication = index == it.second.size - 1)
                        )
                    }
                }
                return@forEach
            }
            //Otherwise, look for matching complication labels or descriptions for this app
            val matchingComplications = it.second.filter { complication ->
                complication.label.contains(searchTerm, true) ||
                        complication.description.contains(searchTerm, true)
            }
            if(matchingComplications.isEmpty()) return@forEach
            complicationList.add(it.first)
            if(it.first.isExpanded) {
                matchingComplications.forEachIndexed { index, complication ->
                    complicationList.add(complication.copy(
                        isLastComplication = index == matchingComplications.size - 1
                    ))
                }
            }
        }
        State.Loaded(complicationList)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        vmScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun onExpandClicked(item: Item.App) {
        item.isExpanded = !item.isExpanded
        vmScope.launch {
            expandBus.emit(System.currentTimeMillis())
        }
    }

    override fun dismiss() {
        vmScope.launch {
            navigation.navigateBack()
        }
    }

    override fun showWidgetPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(ComplicationsAddFragmentDirections.actionComplicationsAddFragmentToWidgetPermissionDialog2(grant))
        }
    }

    override fun showNotificationsPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(ComplicationsAddFragmentDirections.actionComplicationsAddFragmentToNotificationPermissionDialogFragment3(grant))
        }
    }

}