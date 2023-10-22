package com.kieronquinn.app.smartspacer.ui.screens.setup.complications

import android.content.Context
import com.kieronquinn.app.smartspacer.components.navigation.SetupNavigation
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository.CompatibilityReport.Companion.isNativeModeAvailable
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.ui.screens.base.add.complications.BaseAddComplicationsViewModelImpl
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
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseComplication

abstract class SetupComplicationsViewModel(
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

    abstract fun addComplication(complication: Item.Complication)
    abstract fun onNextClicked()

}

class SetupComplicationsViewModelImpl(
    context: Context,
    targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    widgetRepository: WidgetRepository,
    grantRepository: GrantRepository,
    private val navigation: SetupNavigation,
    compatibilityRepository: CompatibilityRepository,
    settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    notificationRepository: NotificationRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): SetupComplicationsViewModel(
    context,
    targetsRepository,
    databaseRepository,
    widgetRepository,
    grantRepository,
    notificationRepository,
    scope,
    dispatcher
) {

    private val nextIndex = databaseRepository.getActions().map { items ->
        (items.maxOfOrNull { it.index } ?: 0) + 1
    }.stateIn(vmScope, SharingStarted.Eagerly, null)

    private val shouldShowNativeSmartspace = flow {
        val enhancedMode = settingsRepository.enhancedMode.get()
        val supported = compatibilityRepository.getCompatibilityReports().isNativeModeAvailable()
        val running = systemSmartspaceRepository.serviceRunning.value
        emit(enhancedMode && supported && !running)
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, null)

    private val recommended = flow {
        val recommended = targetsRepository.getRecommendedComplications().mapNotNull {
            val config = it.getPluginConfig().firstNotNull()
            Item.Complication(
                it.sourcePackage,
                it.authority,
                it.id ?: return@mapNotNull null,
                config.label,
                config.description,
                config.icon,
                config.compatibilityState,
                config.setupActivity,
                config.widgetProvider,
                config.notificationProvider,
                config.broadcastProvider
            )
        }
        emit(recommended)
    }.flowOn(Dispatchers.IO)

    override val state = combine(recommended, databaseRepository.getActions()) { items, database ->
        val addedAuthorities = database.map { it.authority }
        val addedActions = items.map {
            it.copy(isAdded = addedAuthorities.contains(it.authority))
        }
        State.Loaded(addedActions)
    }.stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun addComplication(complication: Item.Complication) {
        vmScope.launch(dispatcher) {
            val nextIndex = nextIndex.firstNotNull()
            val databaseComplication = DatabaseComplication(
                complication.id,
                complication.authority,
                nextIndex,
                complication.packageName
            )
            databaseRepository.addAction(databaseComplication)
        }
    }

    override fun onNextClicked() {
        vmScope.launch {
            if(shouldShowNativeSmartspace.firstNotNull()){
                navigation.navigate(SetupComplicationsFragmentDirections.actionSetupComplicationsFragmentToNativeModeFragment(true))
            }else{
                navigation.navigate(SetupComplicationsFragmentDirections.actionSetupComplicationsFragmentToSetupBatteryOptimisationFragment())
            }
        }
    }

    override fun showWidgetPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(SetupComplicationsFragmentDirections.actionSetupComplicationsFragmentToWidgetPermissionDialogFragment())
        }
    }

    override fun showNotificationsPermissionDialog(grant: Grant) {
        vmScope.launch {
            navigation.navigate(SetupComplicationsFragmentDirections.actionSetupComplicationsFragmentToNotificationPermissionDialogFragment2())
        }
    }

}