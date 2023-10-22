package com.kieronquinn.app.smartspacer.ui.screens.base.add.targets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.model.smartspace.Target.TargetBackup
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.GrantRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
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

abstract class BaseAddTargetsViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val addState: StateFlow<AddState>

    abstract fun onTargetClicked(
        target: Item.Target,
        skipWidgetGrant: Boolean = false,
        skipNotificationGrant: Boolean = false,
        skipRestore: Boolean = false,
        hasRestored: Boolean = false
    )

    abstract fun onWidgetGrantResult(granted: Boolean)
    abstract fun onNotificationGrantResult(granted: Boolean)
    abstract fun onNotificationListenerGrantResult(granted: Boolean)
    abstract fun onTargetConfigureResult(success: Boolean)
    abstract fun onWidgetBindResult(success: Boolean)
    abstract fun onWidgetConfigureResult(success: Boolean)

    abstract fun showWidgetPermissionsDialog(grant: Grant)
    abstract fun showNotificationPermissionsDialog(grant: Grant)
    abstract fun bindAppWidgetIfAllowed(provider: ComponentName, id: Int): Boolean
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
            val target: Item.Target,
            val grant: Grant
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class GrantNotificationListener(
            val target: Item.Target
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class GrantWidgetPermission(
            val target: Item.Target,
            val grant: Grant,
            val skipNotificationGrant: Boolean
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class ConfigureTarget(
            val target: Item.Target
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class BindWidget(
            val target: Item.Target,
            val info: AppWidgetProviderInfo,
            val id: Int
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class ConfigureWidget(
            val target: Item.Target,
            val info: AppWidgetProviderInfo,
            val id: Int
        ): AddState() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
        data class Dismiss(
            val target: Item.Target
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

        data class Target(
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
            val isLastTarget: Boolean = false, //Only used in settings
            val hasCollision: Boolean = false, //Only used in restore
            val backup: TargetBackup? = null //Only used in restore
        ): Item(Type.TARGET)

        enum class Type {
            APP, TARGET
        }
    }

}

abstract class BaseAddTargetsViewModelImpl(
    context: Context,
    private val targetsRepository: TargetsRepository,
    private val databaseRepository: DatabaseRepository,
    private val widgetRepository: WidgetRepository,
    private val grantRepository: GrantRepository,
    private val notificationRepository: NotificationRepository,
    scope: CoroutineScope?,
    private val dispatcher: CoroutineDispatcher
): BaseAddTargetsViewModel(scope) {

    private val packageManager = context.packageManager

    override val addState = MutableStateFlow<AddState>(AddState.Idle)

    override fun onTargetClicked(
        target: Item.Target,
        skipWidgetGrant: Boolean,
        skipNotificationGrant: Boolean,
        skipRestore: Boolean,
        hasRestored: Boolean
    ) {
        vmScope.launch {
            when {
                target.notificationAuthority != null && !skipNotificationGrant &&
                        !target.getGrant().notifications -> {
                    addState.emit(AddState.GrantNotificationPermission(target, target.getGrant()))
                }
                target.notificationAuthority != null &&
                        !notificationRepository.isNotificationListenerEnabled() -> {
                    addState.emit(AddState.GrantNotificationListener(target))
                }
                target.widgetAuthority != null && !skipWidgetGrant && !target.getGrant().widget -> {
                    addState.emit(AddState.GrantWidgetPermission(
                        target, target.getGrant(), skipNotificationGrant
                    ))
                }
                target.backup != null && !skipRestore -> {
                    //Inline: Perform a restore on the target, and then re-call method with skip
                    val restoreSuccess = targetsRepository.performTargetRestore(
                        target.authority,
                        target.id,
                        target.backup.backup
                    )
                    onTargetClicked(
                        target,
                        skipWidgetGrant,
                        skipNotificationGrant,
                        true,
                        restoreSuccess
                    )
                }
                target.setupIntent != null && !hasRestored -> {
                    val configureIntent = target.setupIntent
                    if(packageManager.resolveActivityCompat(configureIntent) != null) {
                        addState.emit(AddState.ConfigureTarget(target))
                    }else{
                        addState.emit(AddState.Dismiss(target))
                    }
                }
                target.widgetAuthority != null -> {
                    val info = target.getWidgetInfo()
                    val id = allocateAppWidgetId()
                    if(info != null){
                        addState.emit(AddState.BindWidget(target, info, id))
                    }else{
                        deallocateAppWidgetId(id)
                        addState.emit(AddState.WidgetError)
                    }
                }
                else -> {
                    addState.emit(AddState.Dismiss(target))
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
                onTargetClicked(
                    target = current.target,
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
            val target = (addState.value as? AddState.GrantNotificationPermission)?.target
                ?: return@launch
            //Slight delay to allow system to catch up
            delay(500L)
            if(granted) {
                //Re-call the click event now the permission has been granted
                onTargetClicked(
                    target,
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
            val target = (addState.value as? AddState.GrantNotificationListener)?.target
                ?: return@launch
            if(granted) {
                //Re-call the click event now the permission has been granted
                onTargetClicked(
                    target,
                    skipNotificationGrant = true,
                    skipWidgetGrant = false
                )
            }else{
                addState.emit(AddState.Idle)
            }
        }
    }

    private suspend fun Item.Target.getGrant(): Grant {
        return grantRepository.getGrantForPackage(packageName) ?: Grant(packageName)
    }

    override fun onTargetConfigureResult(success: Boolean) {
        vmScope.launch {
            val target = (addState.value as? AddState.ConfigureTarget)?.target ?: return@launch
            when {
                !success -> {
                    cancelAddingTarget(target)
                }
                target.widgetAuthority != null -> {
                    val info = target.getWidgetInfo()
                    if(info != null){
                        val id = allocateAppWidgetId()
                        addState.emit(AddState.BindWidget(target, info, id))
                    }else{
                        cancelAddingTarget(target, true)
                    }
                }
                else -> {
                    addState.emit(AddState.Dismiss(target))
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
                    cancelAddingTarget(current.target, true)
                }
                current.info.configure != null -> {
                    addState.emit(AddState.ConfigureWidget(current.target, current.info, current.id))
                }
                else -> {
                    addWidgetToDatabase(current.target, current.info, current.id)
                    addState.emit(AddState.Dismiss(current.target))
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
                    cancelAddingTarget(current.target, true)
                }
                else -> {
                    addWidgetToDatabase(current.target, current.info, current.id)
                    addState.emit(AddState.Dismiss(current.target))
                }
            }
        }
    }

    private suspend fun addWidgetToDatabase(
        target: Item.Target,
        info: AppWidgetProviderInfo,
        id: Int
    ) = withContext(Dispatchers.IO) {
        @Suppress("CloseWidget")
        val widget = Widget(
            target.id,
            Widget.Type.TARGET,
            info.provider.flattenToString(),
            id,
            target.packageName,
            target.widgetAuthority ?: return@withContext
        )
        databaseRepository.addWidget(widget)
    }

    private suspend fun Item.Target.getWidgetInfo(): AppWidgetProviderInfo? {
        return withContext(Dispatchers.IO){
            widgetRepository.getWidgetInfo(widgetAuthority ?: return@withContext null, id)
        }
    }

    private suspend fun cancelAddingTarget(target: Item.Target, isWidget: Boolean = false) {
        val smartspaceTarget = targetsRepository.getAllTargets().firstOrNull {
            it.authority == target.authority && it.id == target.id
        } ?: return
        //Call the delete method for this target, to allow the plugin to clean up if needed
        smartspaceTarget.onDeleted()
        if(isWidget){
            addState.emit(AddState.WidgetError)
        }else{
            addState.emit(AddState.Idle)
        }
        smartspaceTarget.close()
    }

    private fun allocateAppWidgetId(): Int {
        return widgetRepository.allocateAppWidgetId()
    }

    private fun deallocateAppWidgetId(id: Int) {
        widgetRepository.deallocateAppWidgetId(id)
    }

    override fun bindAppWidgetIfAllowed(provider: ComponentName, id: Int): Boolean {
        return widgetRepository.bindAppWidgetIdIfAllowed(id, provider)
    }

    override fun createConfigIntentSender(appWidgetId: Int): IntentSender {
        return widgetRepository.createConfigIntentSender(appWidgetId)
    }

}