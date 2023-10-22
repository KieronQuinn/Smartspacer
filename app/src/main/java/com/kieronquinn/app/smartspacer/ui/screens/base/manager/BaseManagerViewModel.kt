package com.kieronquinn.app.smartspacer.ui.screens.base.manager

import android.content.Context
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.navigation.RootNavigation
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.repositories.DatabaseRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SystemSmartspaceRepository
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.base.BaseViewModel
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerAdapter.ItemHolder
import com.kieronquinn.app.smartspacer.ui.screens.base.manager.BaseManagerViewModel.BaseHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import org.koin.java.KoinJavaComponent.inject
import java.time.Duration
import java.util.Collections

abstract class BaseManagerViewModel<T: BaseHolder>(scope: CoroutineScope?): BaseViewModel(scope) {

    abstract val state: StateFlow<State>

    abstract fun reloadClearingCache()
    abstract fun rerunSetup()
    abstract fun moveItem(from: Int, to: Int)
    abstract fun addItem(
        authority: String,
        id: String,
        packageName: String,
        notificationAuthority: String?,
        broadcastAuthority: String?
    )

    abstract fun onAddClicked()
    abstract fun onItemClicked(item: T)
    abstract fun onOpenItem(item: T)

    abstract fun onStartNativeClicked()
    abstract fun onDonatePromptClicked()

    sealed class State {
        data object Loading: State()
        data class Loaded<T>(val items: List<ItemHolder<T>>, val isEmpty: Boolean): State() {
            override fun equals(other: Any?): Boolean {
                return false
            }
        }
    }

    interface BaseHolder

}

abstract class BaseManagerViewModelImpl<T: BaseHolder, I>(
    context: Context,
    private val smartspaceRepository: SmartspaceRepository,
    private val settingsRepository: SmartspacerSettingsRepository,
    systemSmartspaceRepository: SystemSmartspaceRepository,
    private val databaseRepository: DatabaseRepository,
    scope: CoroutineScope?,
    private val dispatcher: CoroutineDispatcher,
): BaseManagerViewModel<T>(scope) {

    companion object {
        private val MINIMUM_TIME_SINCE_INSTALL_FOR_DONATE_PROMPT = Duration.ofDays(1).toMillis()
        private val MINIMUM_TIME_BETWEEN_DONATE_PROMPTS = Duration.ofDays(14).toMillis()
    }

    abstract fun getCompatibilityState(item: T): CompatibilityState
    abstract suspend fun getHolder(context: Context, databaseItem: I): T

    abstract fun createDatabaseItem(authority: String, id: String, index: Int, packageName: String): I

    abstract fun getDatabaseItem(holder: T): I
    abstract fun getItemId(holder: T): Long

    abstract fun getDatabaseItems(): Flow<List<I>>
    abstract suspend fun addDatabaseItem(item: I)
    abstract suspend fun removeDatabaseItem(item: I)
    abstract suspend fun updateDatabaseItem(item: I)

    abstract fun getDatabaseItemIndex(item: I): Int
    abstract fun setDatabaseItemIndex(item: I, index: Int)

    abstract fun clearInfoCache()
    abstract fun dismissDonatePrompt()

    @VisibleForTesting
    val loadBus = MutableStateFlow(System.currentTimeMillis())

    private val shouldShowNativeReminder = combine(
        settingsRepository.enhancedMode.asFlow(),
        settingsRepository.hasUsedNativeMode.asFlow(),
        systemSmartspaceRepository.serviceRunning
    ) { enhanced, hasUsedNative, running ->
        enhanced && hasUsedNative && !running
    }

    private val shouldShowDonate = combine(
        settingsRepository.donatePromptEnabled.asFlow(),
        settingsRepository.donatePromptDismissedAt.asFlow(),
        settingsRepository.installTime.asFlow(),
        loadBus
    ) { enabled, lastDismiss, installedAt, _ ->
        if(!enabled || installedAt < 0) return@combine false
        val now = System.currentTimeMillis()
        when {
            lastDismiss > 0 -> {
                now - lastDismiss >= MINIMUM_TIME_BETWEEN_DONATE_PROMPTS
            }
            else -> {
                now - installedAt >= MINIMUM_TIME_SINCE_INSTALL_FOR_DONATE_PROMPT
            }
        }
    }

    override val state by lazy {
        combine(
            getDatabaseItems(),
            shouldShowNativeReminder,
            shouldShowDonate,
            loadBus
        ) { databaseItems, showNative, showDonate, _ ->
            val nativeItem = if(showNative){
                ItemHolder.NativeStartReminder<T>(::onStartNativeClicked, ::onDismissNativeClicked)
            } else null
            //Never show both donate and native, to prevent cluttering the UI
            val donateItem = if(showDonate && nativeItem == null){
                ItemHolder.DonatePrompt<T>(::onDonatePromptClicked, ::onDismissDonatePromptClicked)
            } else null
            val managedItems = databaseItems.mapNotNull { item ->
                getHolder(context, item)
            }.map {
                ItemHolder.Item(it, getItemId(it))
            }
            val items = listOfNotNull(nativeItem, donateItem) + managedItems
            val isEmpty = managedItems.isEmpty()
            State.Loaded(items, isEmpty)
        }.flowOn(dispatcher).stateIn(vmScope, SharingStarted.Eagerly, State.Loading)
    }

    override fun reloadClearingCache() {
        vmScope.launch {
            clearInfoCache()
            loadBus.emit(System.currentTimeMillis())
        }
    }

    override fun addItem(
        authority: String,
        id: String,
        packageName: String,
        notificationAuthority: String?,
        broadcastAuthority: String?
    ) {
        vmScope.launch(dispatcher) {
            val items = getDatabaseItems().first()
            val nextIndex = (items.maxOfOrNull { getDatabaseItemIndex(it) } ?: 0) + 1
            if(notificationAuthority != null){
                val notificationListener = NotificationListener(
                    id, packageName, notificationAuthority
                )
                databaseRepository.addNotificationListener(notificationListener)
            }
            if(broadcastAuthority != null){
                val broadcastListener = BroadcastListener(id, packageName, broadcastAuthority)
                databaseRepository.addBroadcastListener(broadcastListener)
            }
            addDatabaseItem(createDatabaseItem(authority, id, nextIndex, packageName))
        }
    }

    override fun onItemClicked(item: T) {
        if(getCompatibilityState(item) == CompatibilityState.Compatible){
            onOpenItem(item)
        }else{
            //User has clicked to delete this unavailable item
            vmScope.launch {
                removeDatabaseItem(getDatabaseItem(item))
            }
        }
    }

    override fun rerunSetup() {
        val navigation by inject<RootNavigation>(RootNavigation::class.java)
        vmScope.launch {
            navigation.navigate(R.id.action_global_setup)
        }
    }

    override fun moveItem(from: Int, to: Int) {
        val adjustment = getAdjustment()
        moveItemInternal(from - adjustment, to - adjustment)
    }

    private fun moveItemInternal(from: Int, to: Int) {
        vmScope.launch(dispatcher) {
            val items = getDatabaseItems().first()
            if (from < to) {
                for (i in from until to) {
                    items.swap(i, i + 1)
                }
            } else {
                for (i in from downTo to + 1) {
                    items.swap(i, i - 1)
                }
            }
            items.forEachIndexed { index, item ->
                setDatabaseItemIndex(item, index)
                updateDatabaseItem(item)
            }
        }
    }

    private fun <I> List<I>.swap(from: Int, to: Int) {
        try {
            Collections.swap(this, from, to)
        }catch (e: IndexOutOfBoundsException){
            //Concurrent modification?
        }
    }

    private fun onDismissNativeClicked() = vmScope.launch {
        settingsRepository.hasUsedNativeMode.set(false)
    }

    private fun onDismissDonatePromptClicked() = vmScope.launch {
        settingsRepository.donatePromptDismissedAt.set(System.currentTimeMillis())
    }

    private fun getAdjustment(): Int {
        return (state.value as? State.Loaded<*>)?.items?.count { it !is ItemHolder.Item<*> } ?: 0
    }

}