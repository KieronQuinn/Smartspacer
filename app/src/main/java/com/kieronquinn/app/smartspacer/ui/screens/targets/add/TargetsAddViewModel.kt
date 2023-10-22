package com.kieronquinn.app.smartspacer.ui.screens.targets.add

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModelImpl
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

abstract class TargetsAddViewModel(
    context: Context,
    targetsRepository: TargetsRepository,
    databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseAddTargetsViewModelImpl(
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

class TargetsAddViewModelImpl(
    context: Context,
    private val targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): TargetsAddViewModel(
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, searchTerm.value.isNotEmpty())

    private val targets = flow {
        val existingTargets = databaseRepository.getTargets().first().map { it.authority }
        val newTargets = targetsRepository.getAllTargets().mapNotNull { target ->
            val config = target.getPluginConfig().firstNotNull()
            if(existingTargets.contains(target.authority) && !config.allowAddingMoreThanOnce){
                return@mapNotNull null
            }
            Item.Target(
                target.sourcePackage,
                target.authority,
                UUID.randomUUID().toString(),
                config.label,
                config.description,
                config.icon,
                config.compatibilityState,
                config.setupActivity,
                config.widgetProvider,
                config.notificationProvider,
                config.broadcastProvider
            ).also {
                target.close()
            }
        }.sortedBy {
            it.label.toString().lowercase()
        }
        val groupedTargets = newTargets.groupBy { it.packageName }.mapKeys {
            val label = packageManager.getPackageLabel(it.key) ?: ""
            Item.App(it.key, label)
        }.toList().sortedBy { it.first.label.toString().lowercase() }
        emit(groupedTargets)
    }.flowOn(Dispatchers.IO)

    override val state = combine(targets, searchTerm, expandBus) { t, s, _ ->
        val targetList = ArrayList<Item>()
        val searchTerm = s.trim()
        t.forEach {
            //If the app's name matches the term, return all its targets
            if(it.first.label.contains(searchTerm, true)) {
                targetList.add(it.first)
                if(it.first.isExpanded) {
                    it.second.forEachIndexed { index, target ->
                        targetList.add(target.copy(isLastTarget = index == it.second.size - 1))
                    }
                }
                return@forEach
            }
            //Otherwise, look for matching target labels or descriptions for this app
            val matchingTargets = it.second.filter { target ->
                target.label.contains(searchTerm, true) ||
                        target.description.contains(searchTerm, true)
            }
            if(matchingTargets.isEmpty()) return@forEach
            targetList.add(it.first)
            if(it.first.isExpanded) {
                matchingTargets.forEachIndexed { index, target ->
                    targetList.add(target.copy(isLastTarget = index == matchingTargets.size - 1))
                }
            }
        }
        State.Loaded(targetList)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    override fun getSearchTerm(): String {
        return searchTerm.value
    }

    override fun setSearchTerm(term: String) {
        viewModelScope.launch {
            searchTerm.emit(term)
        }
    }

    override fun onExpandClicked(item: Item.App) {
        item.isExpanded = !item.isExpanded
        viewModelScope.launch {
            expandBus.emit(System.currentTimeMillis())
        }
    }

    override fun dismiss() {
        viewModelScope.launch {
            navigation.navigateBack()
        }
    }

    override fun showWidgetPermissionsDialog(grant: Grant) {
        viewModelScope.launch {
            navigation.navigate(TargetsAddFragmentDirections.actionTargetsAddFragmentToWidgetPermissionDialog(grant))
        }
    }

    override fun showNotificationPermissionsDialog(grant: Grant) {
        viewModelScope.launch {
            navigation.navigate(TargetsAddFragmentDirections.actionTargetsAddFragmentToNotificationPermissionDialogFragment(grant))
        }
    }

}