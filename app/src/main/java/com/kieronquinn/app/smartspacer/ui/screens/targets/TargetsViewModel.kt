package com.kieronquinn.app.smartspacer.ui.screens.targets

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import com.kieronquinn.app.smartspacer.components.navigation.ContainerNavigation
import com.kieronquinn.app.smartspacer.model.database.Widget
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModelImpl
import com.kieronquinn.app.smartspacer.ui.screens.targets.TargetsViewModel.TargetHolder
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import com.kieronquinn.app.smartspacer.model.database.Target as DatabaseTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Target as SmartspaceTarget

abstract class TargetsViewModel(
    context: Context,
    smartspaceRepository: SmartspaceRepository,
    settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    databaseRepository: DatabaseRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseManagerViewModelImpl<TargetHolder, DatabaseTarget>(
    context,
    smartspaceRepository,
    settingsRepository,
    systemSmartspaceRepository,
    databaseRepository,
    scope,
    dispatcher
) {

    data class TargetHolder(
        val target: DatabaseTarget,
        val info: TargetInfo,
        var isSelected: Boolean = false
    ): BaseHolder

    /**
     *  Holds a cached state of a given target, to prevent having to re-load from the provider every
     *  time it's needed
     */
    data class TargetInfo(
        val icon: Icon,
        val label: CharSequence,
        val description: CharSequence,
        val compatibilityState: CompatibilityState
    )

    abstract fun onWallpaperColourPickerClicked()

}

class TargetsViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    smartspaceRepository: SmartspaceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): TargetsViewModel(
    context,
    smartspaceRepository,
    settingsRepository,
    systemSmartspaceRepository,
    databaseRepository,
    scope,
    dispatcher
) {

    @VisibleForTesting
    val targetInfoCache = HashMap<String, TargetInfo>()

    override fun onAddClicked() {
        vmScope.launch {
            navigation.navigate(TargetsFragmentDirections.actionTargetsFragmentToTargetsAddFragment())
        }
    }

    override fun clearInfoCache() {
        targetInfoCache.clear()
    }

    override fun getCompatibilityState(item: TargetHolder): CompatibilityState {
        return item.info.compatibilityState
    }

    override suspend fun getHolder(
        context: Context,
        databaseItem: DatabaseTarget
    ) = withContext(dispatcher) {
        TargetHolder(databaseItem, getTargetInfo(context, databaseItem))
    }

    override fun getItemId(holder: TargetHolder): Long {
        return holder.target.id.hashCode().toLong()
    }

    override fun createDatabaseItem(
        authority: String,
        id: String,
        index: Int,
        packageName: String
    ): DatabaseTarget {
        return DatabaseTarget(
            authority = authority,
            id = id,
            index = index,
            packageName = packageName
        )
    }

    override fun getDatabaseItem(holder: TargetHolder): DatabaseTarget {
        return holder.target
    }

    override fun getDatabaseItems(): Flow<List<DatabaseTarget>> {
        return databaseRepository.getTargets()
    }

    override suspend fun addDatabaseItem(item: DatabaseTarget) {
        databaseRepository.addTarget(item)
    }

    override suspend fun removeDatabaseItem(item: DatabaseTarget) {
        val requirementIds = item.anyRequirements + item.allRequirements
        //We can't send a delete to the target since it's not available, assume user has uninstalled it
        databaseRepository.deleteTarget(item)
        databaseRepository.deleteWidget(item.id, Widget.Type.TARGET)
        databaseRepository.deleteNotificationListener(item.id)
        databaseRepository.deleteBroadcastListener(item.id)
        requirementIds.forEach {
            databaseRepository.deleteRequirementData(it)
            val requirement = databaseRepository.getRequirementById(it).first() ?: return@forEach
            databaseRepository.deleteRequirement(requirement)
        }
    }

    override suspend fun updateDatabaseItem(item: DatabaseTarget) {
        databaseRepository.updateTarget(item)
    }

    override fun getDatabaseItemIndex(item: DatabaseTarget): Int {
        return item.index
    }

    override fun setDatabaseItemIndex(
        item: DatabaseTarget,
        index: Int
    ) {
        item.index = index
    }

    private suspend fun getTargetInfo(context: Context, target: DatabaseTarget): TargetInfo {
        return targetInfoCache[target.id] ?: loadTargetInfo(context, target).also {
            targetInfoCache[target.id] = it
        }
    }

    private suspend fun loadTargetInfo(context: Context, target: DatabaseTarget): TargetInfo {
        val configTarget = SmartspaceTarget(context, target.authority, target.id)
        val config = configTarget.getPluginConfig().firstNotNull()
        configTarget.close()
        return config.let {
            TargetInfo(
                it.icon,
                it.label,
                it.description,
                it.compatibilityState
            )
        }
    }

    override fun onOpenItem(item: TargetHolder) {
        vmScope.launch {
            navigation.navigate(TargetsFragmentDirections.actionTargetsFragmentToTargetEditFragment(item.target))
        }
    }

    override fun onStartNativeClicked() {
        vmScope.launch {
            navigation.navigate(Uri.parse("smartspacer://native"))
        }
    }

    override fun onWallpaperColourPickerClicked() {
        vmScope.launch {
            navigation.navigate(TargetsFragmentDirections.actionTargetsFragmentToWallpaperColourPickerBottomSheetFragment())
        }
    }

    override fun onDonatePromptClicked() {
        vmScope.launch {
            navigation.navigate(TargetsFragmentDirections.actionTargetsFragmentToNavGraphIncludeDonate())
        }
    }

    override fun dismissDonatePrompt() {
        vmScope.launch {
            settingsRepository.donatePromptDismissedAt.set(System.currentTimeMillis())
        }
    }

}