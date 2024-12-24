package com.kieronquinn.app.smartspacer.ui.screens.base.add.complications

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.resolveActivityCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseAddComplicationsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val addState: StateFlow<AddState>

    abstract fun onComplicationClicked(
        complication: Item.Complication,
        skipWidgetGrant: Boolean = false,
        skipNotificationGrant: Boolean = false,
        skipRestore: Boolean = false,
        hasRestored: Boolean = false
    )

    abstract fun onWidgetGrantResult(granted: Boolean)
    abstract fun onNotificationGrantResult(granted: Boolean)
    abstract fun onNotificationListenerGrantResult(granted: Boolean)
    abstract fun onComplicationConfigureResult(success: Boolean)
    abstract fun onWidgetBindResult(success: Boolean)
    abstract fun onWidgetConfigureResult(success: Boolean)

    abstract fun showWidgetPermissionDialog(grant: Grant)
    abstract fun showNotificationsPermissionDialog(grant: Grant)
    abstract fun bindAppWidgetIfAllowed(
        provider: ComponentName,
        id: Int,
        config: SmartspacerWidgetProvider.Config
    ): Boolean
    abstract fun createConfigIntentSender(appWidgetId: Int): IntentSender

    sealed class State {
        object Loading: State()
        data class Loaded(val items: List<Item>): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class AddState {
        object Idle: AddState()
        data class GrantNotificationPermission(
            val complication: Item.Complication,
            val grant: Grant
        ): AddState()
        data class GrantNotificationListener(
            val complication: Item.Complication,
        ): AddState()
        data class GrantWidgetPermission(
            val complication: Item.Complication,
            val grant: Grant,
            val skipNotificationGrant: Boolean
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class ConfigureComplication(
            val complication: Item.Complication
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class BindWidget(
            val complication: Item.Complication,
            val info: AppWidgetProviderInfo,
            val config: SmartspacerWidgetProvider.Config,
            val id: Int
        ): AddState()
        data class ConfigureWidget(
            val complication: Item.Complication,
            val info: AppWidgetProviderInfo,
            val id: Int
        ): AddState()
        data class Dismiss(
            val complication: Item.Complication
        ): AddState()
        object WidgetError: AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    sealed class Item(val type: Type) {
        data class App(
            val packageName: String,
            val label: CharSequence,
            var isExpanded: Boolean = false
        ): Item(Type.APP)

        data class Complication(
            val packageName: String,
            val authority: String,
            val id: String,
            val label: CharSequence,
            val description: CharSequence,
            val icon: Icon,
            val compatibilityState: CompatibilityState,
            val setupIntent: Intent?,
            val widgetAuthority: String?,
            val notificationAuthority: String?,
            val broadcastAuthority: String?,
            val isAdded: Boolean = false, //Only used during setup
            val isLastComplication: Boolean = false, //Only used in settings
            val hasCollision: Boolean = false, //Only used in restore
            val backup: Action.ComplicationBackup? = null //Only used in restore
        ): Item(Type.COMPLICATION)

        enum class Type {
            APP, COMPLICATION
        }
    }

}

abstract class BaseAddComplicationsViewModelImpl(
    context: Context,
    private val targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    private val widgetRepository: WidgetRepository,
    private val grantRepository: GrantRepository,
    private val notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): BaseAddComplicationsViewModel(scope) {

    private val packageManager = context.packageManager

    override val addState = MutableStateFlow<AddState>(AddState.Idle)

    override fun onComplicationClicked(
        complication: Item.Complication,
        skipWidgetGrant: Boolean,
        skipNotificationGrant: Boolean,
        skipRestore: Boolean,
        hasRestored: Boolean
    ) {
        vmScope.launch {
            when {
                complication.notificationAuthority != null && !skipNotificationGrant &&
                        !complication.getGrant().notifications -> {
                    addState.emit(AddState.GrantNotificationPermission(complication, complication.getGrant()))
                }
                complication.notificationAuthority != null &&
                        !notificationRepository.isNotificationListenerEnabled() -> {
                    addState.emit(AddState.GrantNotificationListener(complication))
                }
                complication.widgetAuthority != null && !skipWidgetGrant && !complication.getGrant().widget -> {
                    addState.emit(AddState.GrantWidgetPermission(
                        complication, complication.getGrant(), skipNotificationGrant
                    ))
                }
                complication.backup != null && !skipRestore -> {
                    //Inline: Perform a restore on the complication, and then re-call method with skip
                    val restoreSuccess = targetsRepository.performComplicationRestore(
                        complication.authority,
                        complication.id,
                        complication.backup.backup
                    )
                    onComplicationClicked(
                        complication,
                        skipWidgetGrant,
                        skipNotificationGrant,
                        true,
                        restoreSuccess
                    )
                }
                complication.setupIntent != null && !hasRestored -> {
                    val configureIntent = complication.setupIntent
                    if(packageManager.resolveActivityCompat(configureIntent) != null) {
                        addState.emit(AddState.ConfigureComplication(complication))
                    }else{
                        addState.emit(AddState.Dismiss(complication))
                    }
                }
                complication.widgetAuthority != null -> {
                    val info = complication.getWidgetInfo()
                    val id = allocateAppWidgetId()
                    val config = complication.getWidgetConfig()
                    if(info != null && config != null){
                        addState.emit(AddState.BindWidget(complication, info, config, id))
                    }else{
                        deallocateAppWidgetId(id)
                        addState.emit(AddState.WidgetError)
                    }
                }
                else -> {
                    addState.emit(AddState.Dismiss(complication))
                }
            }
        }
    }

    override fun onComplicationConfigureResult(success: Boolean) {
        vmScope.launch {
            val complication = (addState.value as? AddState.ConfigureComplication)?.complication ?: return@launch
            when {
                !success -> {
                    cancelAddingComplication(complication)
                }
                complication.widgetAuthority != null -> {
                    val info = complication.getWidgetInfo()
                    val config = complication.getWidgetConfig()
                    if(info != null && config != null){
                        val id = allocateAppWidgetId()
                        addState.emit(AddState.BindWidget(complication, info, config, id))
                    }else{
                        cancelAddingComplication(complication)
                    }
                }
                else -> {
                    addState.emit(AddState.Dismiss(complication))
                }
            }
        }
    }

    override fun onWidgetBindResult(success: Boolean) {
        vmScope.launch {
            val current = (addState.value as? AddState.BindWidget) ?: return@launch
            when {
                !success -> {
                    deallocateAppWidgetId(current.id)
                    cancelAddingComplication(current.complication)
                }
                current.info.configure != null -> {
                    addState.emit(AddState.ConfigureWidget(current.complication, current.info, current.id))
                }
                else -> {
                    addWidgetToDatabase(current.complication, current.info, current.id)
                    addState.emit(AddState.Dismiss(current.complication))
                }
            }
        }
    }

    override fun onWidgetGrantResult(granted: Boolean) {
        vmScope.launch {
            val current = addState.value as? AddState.GrantWidgetPermission ?: return@launch
            //Slight delay to allow system to catch up
            delay(500L)
            if(granted) {
                //Re-call the click event now the permission has been granted
                onComplicationClicked(
                    complication = current.complication,
                    skipWidgetGrant = true,
                    skipNotificationGrant = current.skipNotificationGrant
                )
            }else{
                addState.emit(AddState.Idle)
            }
        }
    }

    override fun onNotificationGrantResult(granted: Boolean) {
        vmScope.launch {
            val complication = (addState.value as? AddState.GrantNotificationPermission)?.complication
                ?: return@launch
            //Slight delay to allow system to catch up
            delay(500L)
            if(granted) {
                //Re-call the click event now the permission has been granted
                onComplicationClicked(
                    complication,
                    skipNotificationGrant = true,
                    skipWidgetGrant = false
                )
            }else{
                addState.emit(AddState.Idle)
            }
        }
    }

    override fun onNotificationListenerGrantResult(granted: Boolean) {
        vmScope.launch {
            val complication = (addState.value as? AddState.GrantNotificationListener)?.complication
                ?: return@launch
            if(granted) {
                //Re-call the click event now the permission has been granted
                onComplicationClicked(
                    complication,
                    skipNotificationGrant = true,
                    skipWidgetGrant = false
                )
            }else{
                addState.emit(AddState.Idle)
            }
        }
    }

    private suspend fun Item.Complication.getGrant(): Grant {
        return grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
    }

    override fun onWidgetConfigureResult(success: Boolean) {
        vmScope.launch {
            val current = (addState.value as? AddState.ConfigureWidget) ?: return@launch
            when {
                !success -> {
                    deallocateAppWidgetId(current.id)
                    cancelAddingComplication(current.complication)
                }
                else -> {
                    addWidgetToDatabase(current.complication, current.info, current.id)
                    addState.emit(AddState.Dismiss(current.complication))
                }
            }
        }
    }

    private suspend fun addWidgetToDatabase(
        complication: Item.Complication,
        info: AppWidgetProviderInfo,
        id: Int
    ) = withContext(dispatcher) {
        @Suppress("CloseWidget")
        val widget = Widget(
            complication.id,
            Widget.Type.COMPLICATION,
            info.provider.flattenToString(),
            id,
            complication.packageName,
            complication.widgetAuthority ?: return@withContext
        )
        databaseRepository.addWidget(widget)
    }

    private suspend fun Item.Complication.getWidgetInfo(): AppWidgetProviderInfo? {
        return withContext(dispatcher){
            widgetRepository.getWidgetInfo(widgetAuthority ?: return@withContext null, id)
        }
    }

    private suspend fun Item.Complication.getWidgetConfig(): SmartspacerWidgetProvider.Config? {
        return withContext(dispatcher) {
            widgetRepository.getWidgetConfig(widgetAuthority ?: return@withContext null, id)
        }
    }

    private suspend fun cancelAddingComplication(complication: Item.Complication) {
        val smartspaceTarget = targetsRepository.getAllTargets().firstOrNull {
            it.authority == complication.authority && it.id == complication.id
        } ?: return
        //Call the delete method for this target, to allow the plugin to clean up if needed
        smartspaceTarget.onDeleted()
        smartspaceTarget.close()
        addState.emit(AddState.WidgetError)
    }

    private fun allocateAppWidgetId(): Int {
        return widgetRepository.allocateAppWidgetId()
    }

    private fun deallocateAppWidgetId(id: Int) {
        widgetRepository.deallocateAppWidgetId(id)
    }

    override fun bindAppWidgetIfAllowed(
        provider: ComponentName,
        id: Int,
        config: SmartspacerWidgetProvider.Config
    ): Boolean {
        return widgetRepository.bindAppWidgetIdIfAllowed(id, provider, config)
    }

    override fun createConfigIntentSender(appWidgetId: Int): IntentSender {
        return widgetRepository.createConfigIntentSender(appWidgetId)
    }

}