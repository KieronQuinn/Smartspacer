package com.kieronquinn.app.smartspacer.ui.screens.complications.edit

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.os.BuildCompat
import com.kieronquinn.app.smartspacer.Smartspacer.Companion.PACKAGE_KEYGUARD
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.OemSmartspacerRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.TargetsRepository
import com.kieronquinn.app.smartspacer.repositories.WidgetRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import com.kieronquinn.app.smartspacer.utils.extensions.resolveActivityCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.model.database.Action as Complication
import com.kieronquinn.app.smartspacer.model.smartspace.Action as SmartspaceAction

abstract class ComplicationEditViewModel(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract fun setupWithComplication(complication: Complication)
    abstract val state: StateFlow<State>

    abstract fun onShowOnHomeChanged(enabled: Boolean)
    abstract fun onShowOnLockChanged(enabled: Boolean)
    abstract fun onShowOnExpandedChanged(enabled: Boolean)
    abstract fun onShowOnMusicChanged(enabled: Boolean)
    abstract fun onExpandedShowWhenLockedChanged(enabled: Boolean)
    abstract fun onRequirementsClicked()
    abstract fun notifyChangeAfterDelay()

    abstract fun onDeleteClicked()

    sealed class State {
        object Loading: State()
        data class Loaded(val complication: ComplicationHolder): State()
    }

    data class ConfigActivityInfo(
        val label: CharSequence?,
        val description: CharSequence?,
        val icon: Drawable?,
        val componentName: ComponentName
    )

    data class ComplicationHolder(
        val complication: Complication,
        val smartspaceComplication: SmartspaceAction,
        val expandedModeEnabled: Boolean,
        val nativeHomeAvailable: Boolean,
        val oemHomeAvailable: Boolean,
        val nativeLockAvailable: Boolean,
        val nativeMusicAvailable: Boolean,
        val oemLockAvailable: Boolean,
        val providerPackageLabel: CharSequence,
        val configInfo: ConfigActivityInfo?,
        val config: SmartspacerComplicationProvider.Config,
    )

}

class ComplicationEditViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    private val targetsRepository: TargetsRepository,
    private val widgetRepository: WidgetRepository,
    private val oemSmartspacerRepository: OemSmartspacerRepository,
    settingsRepository: SmartspacerSettingsRepository,
    compatibilityRepository: CompatibilityRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): ComplicationEditViewModel(scope) {

    private val complicationId = MutableStateFlow<String?>(null)

    private val complication = complicationId.filterNotNull().flatMapLatest {
        databaseRepository.getActionById(it)
    }

    private val packageManager = context.packageManager

    @OptIn(BuildCompat.PrereleaseSdkCheck::class)
    override val state = complication.filterNotNull().mapNotNull {
        val smartspaceAction = SmartspaceAction(context, it.authority, it.id, it.packageName)
        val config = smartspaceAction.getPluginConfig().firstNotNull()
        val expandedMode = settingsRepository.expandedModeEnabled.get()
        val nativePreviouslyUsed = settingsRepository.enhancedMode.get()
                && settingsRepository.hasUsedNativeMode.get()
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
        State.Loaded(ComplicationHolder(
            it,
            smartspaceAction,
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
    }.flowOn(Dispatchers.IO).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)

    override fun setupWithComplication(complication: Complication) {
        vmScope.launch {
            this@ComplicationEditViewModelImpl.complicationId.emit(complication.id)
        }
    }

    override fun onShowOnHomeChanged(enabled: Boolean) {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            databaseRepository.updateActionConfig(complication.complication.id){
                it.showOnHomeScreen = enabled
            }
        }
    }

    override fun onShowOnLockChanged(enabled: Boolean) {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            databaseRepository.updateActionConfig(complication.complication.id){
                it.showOnLockScreen = enabled
            }
        }
    }

    override fun onShowOnExpandedChanged(enabled: Boolean) {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            databaseRepository.updateActionConfig(complication.complication.id){
                it.showOnExpanded = enabled
            }
        }
    }

    override fun onShowOnMusicChanged(enabled: Boolean) {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            databaseRepository.updateActionConfig(complication.complication.id){
                it.showOnMusic = enabled
            }
        }
    }

    override fun onExpandedShowWhenLockedChanged(enabled: Boolean) {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            databaseRepository.updateActionConfig(complication.complication.id){
                it.expandedShowWhenLocked = enabled
            }
        }
    }

    override fun onRequirementsClicked() {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            navigation.navigate(
                ComplicationEditFragmentDirections.actionComplicationEditFragmentToComplicationsRequirementsFragment(
                    complication.complication.id
                )
            )
        }
    }

    override fun notifyChangeAfterDelay() {
        vmScope.launch {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            targetsRepository.notifyComplicationChangeAfterDelay(
                complication.complication.id, complication.complication.authority
            )
        }
    }

    override fun onDeleteClicked() {
        vmScope.launch(dispatcher) {
            val complication = (state.value as? State.Loaded)?.complication ?: return@launch
            complication.smartspaceComplication.onDeleted()
            val requirementIds = complication.complication.anyRequirements +
                    complication.complication.allRequirements
            databaseRepository.deleteAction(complication.complication)
            val widget = widgetRepository.widgets.first()?.firstOrNull {
                it.id == complication.complication.id && it.type == Widget.Type.COMPLICATION
            }
            if(widget != null){
                widget.onDeleted()
                databaseRepository.deleteWidget(widget.id, Widget.Type.COMPLICATION)
            }
            databaseRepository.deleteNotificationListener(complication.complication.id)
            databaseRepository.deleteBroadcastListener(complication.complication.id)
            requirementIds.forEach {
                databaseRepository.deleteRequirementData(it)
                val requirement = databaseRepository.getRequirementById(it).first() ?: return@forEach
                databaseRepository.deleteRequirement(requirement)
            }
            navigation.navigateBack()
        }
    }

    private fun ActivityInfo.loadConfigActivityInfo(context: Context): ConfigActivityInfo {
        //We only want the icon & label if they're directly set, rather than falling back to the app
        val packageResources = packageManager.getResourcesForApplication(packageName)
        return ConfigActivityInfo(
            if (labelRes != 0) packageResources.getText(labelRes) else null,
            if (descriptionRes != 0) packageResources.getText(descriptionRes) else null,
            if (icon != 0) Icon.createWithResource(packageName, icon)
                .loadDrawable(context) else null,
            ComponentName(packageName, name)
        )
    }

}