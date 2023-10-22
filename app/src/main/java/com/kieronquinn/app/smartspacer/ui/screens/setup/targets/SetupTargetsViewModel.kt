package com.kieronquinn.app.smartspacer.ui.screens.setup.targets

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.add.targets.BaseAddTargetsViewModelImpl
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget

abstract class SetupTargetsViewModel(
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

    abstract fun addTarget(target: Item.Target)
    abstract fun onNextClicked()

}

class SetupTargetsViewModelImpl(
    context: Context,
    targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    private val navigation: SetupNavigation,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): SetupTargetsViewModel(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    private val nextIndex = databaseRepository.getTargets().map { items ->
        (items.maxOfOrNull { it.index } ?: 0) + 1
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val recommended = flow {
        val recommended = targetsRepository.getRecommendedTargets().mapNotNull { target ->
            val config = target.getPluginConfig().firstNotNull()
            Item.Target(
                target.sourcePackage,
                target.authority,
                target.id ?: return@mapNotNull null,
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
        }
        emit(recommended)
    }.flowOn(Dispatchers.IO)

    override val state = combine(recommended, databaseRepository.getTargets()) { items, database ->
        val addedAuthorities = database.map { it.authority }
        val addedItems = items.map {
            it.copy(isAdded = addedAuthorities.contains(it.authority))
        }
        State.Loaded(addedItems)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun addTarget(target: Item.Target) {
        vmScope.launch(Dispatchers.IO) {
            val nextIndex = nextIndex.firstNotNull()
            val databaseTarget = DatabaseTarget(
                target.id,
                target.authority,
                nextIndex,
                target.packageName
            )
            if(target.notificationAuthority != null){
                val notificationListener = NotificationListener(
                    target.id, target.packageName, target.notificationAuthority
                )
                databaseRepository.addNotificationListener(notificationListener)
            }
            if(target.broadcastAuthority != null){
                val broadcastListener = BroadcastListener(
                    target.id, target.packageName, target.broadcastAuthority
                )
                databaseRepository.addBroadcastListener(broadcastListener)
            }
            databaseRepository.addTarget(databaseTarget)
        }
    }

    override fun onNextClicked() {
        vmScope.launch {
            navigation.navigate(SetupTargetsFragmentDirections.actionSetupTargetsFragmentToSetupComplicationsFragment())
        }
    }

    override fun showWidgetPermissionsDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(SetupTargetsFragmentDirections.actionSetupTargetsFragmentToWidgetPermissionDialogFragment())
        }
    }

    override fun showNotificationPermissionsDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(SetupTargetsFragmentDirections.actionSetupTargetsFragmentToNotificationPermissionDialogFragment2())
        }
    }

}