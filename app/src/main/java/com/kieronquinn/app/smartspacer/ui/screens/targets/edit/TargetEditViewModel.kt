package com.kieronquinn.app.smartspacer.ui.screens.targets.edit

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.os.BuildCompat
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.resolveActivityCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.model.smartspace.Target as SmartspaceTarget

abstract class TargetEditViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun setupWithTarget(target: Target)
    abstract val state: StateFlow<State>

    abstract fun onShowOnHomeChanged(enabled: Boolean)
    abstract fun onShowOnLockChanged(enabled: Boolean)
    abstract fun onShowOnExpandedChanged(enabled: Boolean)
    abstract fun onShowOnMusicChanged(enabled: Boolean)
    abstract fun onExpandedShowWhenLockedChanged(enabled: Boolean)
    abstract fun onDisableSubComplicationsChanged(enabled: Boolean)
    abstract fun onShowWidgetChanged(enabled: Boolean)
    abstract fun onShowRemoteViewsChanged(enabled: Boolean)
    abstract fun onShowShortcutsChanged(enabled: Boolean)
    abstract fun onShowAppShortcutsChanged(enabled: Boolean)
    abstract fun onRequirementsClicked()
    abstract fun notifyChangeAfterDelay()

    abstract fun onDeleteClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val target: TargetHolder): State()
    }

    data class ConfigActivityInfo(
        val label: CharSequence?,
        val description: CharSequence?,
        val icon: Drawable?,
        val componentName: ComponentName
    )

    data class TargetHolder(
        val target: Target,
        val smartspaceTarget: SmartspaceTarget,
        val enhancedModeEnabled: Boolean,
        val expandedModeEnabled: Boolean,
        val nativeHomeAvailable: Boolean,
        val oemHomeAvailable: Boolean,
        val nativeLockAvailable: Boolean,
        val nativeMusicAvailable: Boolean,
        val oemLockAvailable: Boolean,
        val providerPackageLabel: CharSequence,
        val configInfo: ConfigActivityInfo?,
        val config: SmartspacerTargetProvider.Config,
    )

}

class TargetEditViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val widgetRepository: WidgetRepository,
    private val navigation: ContainerNavigation,
    private val targetsRepository: TargetsRepository,
    private val oemSmartspacerRepository: OemSmartspacerRepository,
    private val expandedRepository: ExpandedRepository,
    settingsRepository: SmartspacerSettingsRepository,
    compatibilityRepository: CompatibilityRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): TargetEditViewModel(scope) {

    private val targetId = MutableSharedFlow<String>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val target = targetId.flatMapLatest {
        databaseRepository.getTargetById(it)
    }

    private val packageManager = context.packageManager

    @OptIn(BuildCompat.PrereleaseSdkCheck::class)
    override val state = target.mapNotNull {
        if(it == null) return@mapNotNull null
        val smartspaceTarget = SmartspaceTarget(context, it.authority, it.id, it.packageName)
        val config = smartspaceTarget.getPluginConfig().firstNotNull()
        val enhancedMode = settingsRepository.enhancedMode.get()
        val expandedMode = settingsRepository.expandedModeEnabled.get()
        val nativePreviouslyUsed = enhancedMode && settingsRepository.hasUsedNativeMode.get()
        val reports = compatibilityRepository.getCompatibilityReports()
        val defaultLauncher = context.getDefaultLauncher()
        val nativeHomeAvailable = nativePreviouslyUsed &&
                reports.any { packageName -> packageName.packageName == defaultLauncher }
        val nativeLockAvailable = nativePreviouslyUsed && reports.any { packageName ->
            packageName.packageName == PACKAGE_KEYGUARD
        }
        val nativeMusicAvailable = !BuildCompat.isAtLeastV()
        val oemLockAvailable: Boolean
        val oemHomeAvailable: Boolean
        if(!nativeHomeAvailable || !nativeLockAvailable) {
            val oemApps = oemSmartspacerRepository.getCompatibleApps().first()
            oemLockAvailable = oemApps.any { app -> app.packageName == PACKAGE_KEYGUARD }
            oemHomeAvailable = oemApps.any { app -> app.packageName != PACKAGE_KEYGUARD }
        }else{
            //SPEED HACK - Checking apps is slow on 14+, skip if possible - using native anyway
            oemHomeAvailable = false
            oemLockAvailable = false
        }
        val packageLabel = packageManager.getPackageLabel(it.packageName)
            ?: return@mapNotNull null
        val configInfo = config.configActivity?.let { intent ->
            packageManager.resolveActivityCompat(intent)?.activityInfo
        }?.loadConfigActivityInfo(context)
        State.Loaded(TargetHolder(
            it,
            smartspaceTarget,
            enhancedMode,
            expandedMode,
            nativeHomeAvailable,
            oemHomeAvailable,
            nativeLockAvailable,
            nativeMusicAvailable,
            oemLockAvailable,
            packageLabel,
            configInfo,
            config
        ))
    }.flowOn(dispatcher).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithTarget(target: Target) {
        vmScope.launch {
            this@TargetEditViewModelImpl.targetId.emit(target.id)
        }
    }

    override fun onShowOnHomeChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showOnHomeScreen = enabled
            }
        }
    }

    override fun onShowOnLockChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showOnLockScreen = enabled
            }
        }
    }

    override fun onShowOnExpandedChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showOnExpanded = enabled
            }
        }
    }

    override fun onShowOnMusicChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showOnMusic = enabled
            }
        }
    }

    override fun onExpandedShowWhenLockedChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.expandedShowWhenLocked = enabled
            }
        }
    }

    override fun onDisableSubComplicationsChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.disableSubComplications = enabled
            }
        }
    }

    override fun onShowWidgetChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showWidget = enabled
            }
        }
    }

    override fun onShowRemoteViewsChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showRemoteViews = enabled
            }
        }
    }

    override fun onShowShortcutsChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showShortcuts = enabled
            }
        }
    }

    override fun onShowAppShortcutsChanged(enabled: Boolean) {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            databaseRepository.updateTargetConfig(target.target.id){
                it.showAppShortcuts = enabled
            }
        }
    }

    override fun onRequirementsClicked() {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            navigation.navigate(
                TargetEditFragmentDirections.actionTargetEditFragmentToTargetsRequirementsFragment(
                    target.target.id
                )
            )
        }
    }

    override fun notifyChangeAfterDelay() {
        vmScope.launch {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            targetsRepository.notifyTargetChangeAfterDelay(
                target.target.id, target.target.authority
            )
        }
    }

    override fun onDeleteClicked() {
        vmScope.launch(dispatcher) {
            val target = (state.value as? State.Loaded)?.target ?: return@launch
            target.smartspaceTarget.onDeleted()
            val requirementIds = target.target.anyRequirements + target.target.allRequirements
            databaseRepository.deleteTarget(target.target)
            val widget = widgetRepository.widgets.first()?.firstOrNull {
                it.id == target.target.id && it.type == Widget.Type.TARGET
            }
            if(widget != null){
                widget.onDeleted()
                databaseRepository.deleteWidget(widget.id, Widget.Type.TARGET)
            }
            expandedRepository.removeAppWidget(target.target.id)
            databaseRepository.deleteNotificationListener(target.target.id)
            databaseRepository.deleteBroadcastListener(target.target.id)
            requirementIds.forEach {
                databaseRepository.deleteRequirementData(it)
                val requirement = databaseRepository.getRequirementById(it).first() ?: return@forEach
                databaseRepository.deleteRequirement(requirement)
            }
            navigation.navigateBack()
        }
    }

    override fun onCleared() {
        super.onCleared()
        (state.value as? State.Loaded)?.target?.smartspaceTarget?.close()
    }

    private fun ActivityInfo.loadConfigActivityInfo(context: Context): ConfigActivityInfo {
        //We only want the icon & label if they're directly set, rather than falling back to the app
        val packageResources = packageManager.getResourcesForApplication(packageName)
        return ConfigActivityInfo(
            if(labelRes != 0) packageResources.getText(labelRes) else null,
            if(descriptionRes != 0) packageResources.getText(descriptionRes) else null,
            if(icon != 0) Icon.createWithResource(packageName, icon).loadDrawable(context) else null,
            ComponentName(packageName, name)
        )
    }

}