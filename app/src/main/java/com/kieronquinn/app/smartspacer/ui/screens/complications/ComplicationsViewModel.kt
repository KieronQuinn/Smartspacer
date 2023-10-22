package com.kieronquinn.app.smartspacer.ui.screens.complications

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
import com.kieronquinn.app.smartspacer.ui.screens.complications.ComplicationsViewModel.ComplicationHolder
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import com.kieronquinn.app.smartspacer.model.database.Action as DatabaseAction
import com.kieronquinn.app.smartspacer.model.smartspace.Action as SmartspaceAction

abstract class ComplicationsViewModel(
    context: Context,
    smartspaceRepository: SmartspaceRepository,
    settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    databaseRepository: DatabaseRepository,
    scope: CoroutineScope?,
    dispatcher: CoroutineDispatcher
): BaseManagerViewModelImpl<ComplicationHolder, DatabaseAction>(
    context,
    smartspaceRepository,
    settingsRepository,
    systemSmartspaceRepository,
    databaseRepository,
    scope,
    dispatcher
) {

    data class ComplicationHolder(
        val complication: DatabaseAction,
        val info: ComplicationInfo,
        var isSelected: Boolean = false
    ): BaseHolder

    /**
     *  Holds a cached state of a given complication, to prevent having to re-load from the provider
     *  every time it's needed
     */
    data class ComplicationInfo(
        val icon: Icon,
        val label: CharSequence,
        val description: CharSequence,
        val compatibilityState: CompatibilityState
    )

    abstract fun onWallpaperColourPickerClicked()

}

class ComplicationsViewModelImpl(
    context: Context,
    private val databaseRepository: DatabaseRepository,
    private val navigation: ContainerNavigation,
    smartspaceRepository: SmartspaceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): ComplicationsViewModel(
    context,
    smartspaceRepository,
    settingsRepository,
    systemSmartspaceRepository,
    databaseRepository,
    scope,
    dispatcher
) {

    @VisibleForTesting
    val complicationInfoCache = HashMap<String, ComplicationInfo>()

    override fun onAddClicked() {
        vmScope.launch {
            navigation.navigate(ComplicationsFragmentDirections.actionComplicationsFragmentToComplicationsAddFragment())
        }
    }

    override fun clearInfoCache() {
        complicationInfoCache.clear()
    }

    override fun getCompatibilityState(item: ComplicationHolder): CompatibilityState {
        return item.info.compatibilityState
    }

    override suspend fun getHolder(
        context: Context,
        databaseItem: DatabaseAction
    ): ComplicationHolder {
        return withContext(dispatcher) {
            ComplicationHolder(databaseItem, getComplicationInfo(context, databaseItem))
        }
    }

    override fun getItemId(holder: ComplicationHolder): Long {
        return holder.complication.id.hashCode().toLong()
    }

    private suspend fun getComplicationInfo(context: Context, complication: DatabaseAction): ComplicationInfo {
        return complicationInfoCache[complication.id] ?: loadComplicationInfo(
            context, complication
        ).also {
            complicationInfoCache[complication.id] = it
        }
    }

    private suspend fun loadComplicationInfo(
        context: Context,
        complication: DatabaseAction
    ): ComplicationInfo {
        val action = SmartspaceAction(context, complication.authority, complication.id)
        val config = action.getPluginConfig().firstNotNull().also {
            action.close()
        }
        return config.let {
            ComplicationInfo(
                it.icon,
                it.label,
                it.description,
                it.compatibilityState
            )
        }
    }

    override fun createDatabaseItem(
        authority: String,
        id: String,
        index: Int,
        packageName: String
    ): DatabaseAction {
        return DatabaseAction(
            authority = authority,
            id = id,
            index = index,
            packageName = packageName
        )
    }

    override fun getDatabaseItem(holder: ComplicationHolder): DatabaseAction {
        return holder.complication
    }

    override fun getDatabaseItems(): Flow<List<DatabaseAction>> {
        return databaseRepository.getActions()
    }

    override suspend fun addDatabaseItem(item: DatabaseAction) {
        databaseRepository.addAction(item)
    }

    override suspend fun removeDatabaseItem(item: DatabaseAction) {
        val requirementIds = item.anyRequirements + item.allRequirements
        //We can't send a delete to the complication since it's not available, assume user has uninstalled it
        databaseRepository.deleteAction(item)
        databaseRepository.deleteWidget(item.id, Widget.Type.COMPLICATION)
        databaseRepository.deleteNotificationListener(item.id)
        databaseRepository.deleteBroadcastListener(item.id)
        requirementIds.forEach {
            databaseRepository.deleteRequirementData(it)
            val requirement = databaseRepository.getRequirementById(it).first() ?: return@forEach
            databaseRepository.deleteRequirement(requirement)
        }
    }

    override suspend fun updateDatabaseItem(item: DatabaseAction) {
        databaseRepository.updateAction(item)
    }

    override fun getDatabaseItemIndex(item: DatabaseAction): Int {
        return item.index
    }

    override fun setDatabaseItemIndex(
        item: DatabaseAction,
        index: Int
    ) {
        item.index = index
    }

    override fun onOpenItem(item: ComplicationHolder) {
        vmScope.launch {
            navigation.navigate(ComplicationsFragmentDirections.actionComplicationsFragmentToComplicationEditFragment(item.complication))
        }
    }

    override fun onStartNativeClicked() {
        vmScope.launch {
            navigation.navigate(Uri.parse("smartspacer://native"))
        }
    }

    override fun onWallpaperColourPickerClicked() {
        vmScope.launch {
            navigation.navigate(ComplicationsFragmentDirections.actionComplicationsFragmentToWallpaperColourPickerBottomSheetFragment2())
        }
    }

    override fun onDonatePromptClicked() {
        vmScope.launch {
            navigation.navigate(ComplicationsFragmentDirections.actionComplicationsFragmentToNavGraphIncludeDonate())
        }
    }

    override fun dismissDonatePrompt() {
        vmScope.launch {
            settingsRepository.donatePromptDismissedAt.set(System.currentTimeMillis())
        }
    }

}