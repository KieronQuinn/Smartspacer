package com.kieronquinn.app.smartspacer.components.smartspace

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.smartspace.ActionHolder
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.CompatibilityRepository
import com.kieronquinn.app.smartspacer.repositories.MediaRepository
import com.kieronquinn.app.smartspacer.repositories.ShizukuServiceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.HideSensitive
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.utils.extensions.audioPlaying
import com.kieronquinn.app.smartspacer.utils.extensions.deepEquals
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.handleLifecycleEventSafely
import com.kieronquinn.app.smartspacer.utils.extensions.notificationServiceEnabled
import com.kieronquinn.app.smartspacer.utils.extensions.replaceActionsWithExpanded
import com.kieronquinn.app.smartspacer.utils.extensions.screenOff
import com.kieronquinn.app.smartspacer.utils.extensions.shouldShowOnSurface
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseSmartspacerSession<T, I>(
    context: Context,
    config: SmartspaceConfig,
    open val sessionId: I
): KoinComponent, LifecycleOwner {

    val createdAt: Long = System.currentTimeMillis()

    protected val settings by inject<SmartspacerSettingsRepository>()

    private val shizukuRepository by inject<ShizukuServiceRepository>()
    private val mediaRepository by inject<MediaRepository>()
    private val smartspaceRepository by inject<SmartspaceRepository>()
    private val compatibilityRepository by inject<CompatibilityRepository>()

    private val lastUpdateTime = HashMap<String, Long>()
    private val isVisible = MutableStateFlow(false)
    private val forceReloadBus = MutableStateFlow(System.currentTimeMillis())
    private val contentHiddenString = context.getString(R.string.target_sensitive_title)

    open val uiSurface = flowOf(config.uiSurface)
    open val enablePeriodicUpdates = true
    open val supportsAodAudio = true

    private val dispatcher by lazy {
        LifecycleRegistry(this)
    }

    override val lifecycle
        get() = dispatcher

    private val smartspaceSessionId by lazy {
        toSmartspacerSessionId(sessionId)
    }

    private val mediaPlaying = context.notificationServiceEnabled().flatMapLatest { enabled ->
        if(enabled){
            combine(mediaRepository.mediaPlaying, context.audioPlaying()) { media, audio ->
                media && audio
            }
        }else{
            context.audioPlaying()
        }
    }

    private val aodAudio by lazy {
        if(supportsAodAudio) {
            combine(context.screenOff(), mediaPlaying) { screen, audio ->
                screen && audio
            }.distinctUntilChanged()
        }else flowOf(false)
    }

    private val expandedOpenMode by lazy {
        combine(
            uiSurface,
            settings.expandedModeEnabled.asFlow(),
            settings.expandedOpenModeHome.asFlow(),
            settings.expandedOpenModeLock.asFlow()
        ) { surface, enabled, home, lock ->
            when {
                !enabled -> ExpandedOpenMode.NEVER
                surface == UiSurface.HOMESCREEN -> home
                surface == UiSurface.LOCKSCREEN -> lock
                surface == UiSurface.MEDIA_DATA_MANAGER -> ExpandedOpenMode.NEVER
                surface == UiSurface.GLANCEABLE_HUB -> ExpandedOpenMode.NEVER
                else -> throw RuntimeException("Invalid expanded open mode")
            }
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, null)
    }

    private val supportsSplitSmartspace = flow {
        emit(compatibilityRepository.doesSystemUISupportSplitSmartspace())
    }

    open val actionsFirst = flowOf(false)

    private val sessionSettings by lazy {
        combine(
            settings.hideSensitive.asFlow(),
            settings.nativeUseSplitSmartspace.asFlow(),
            supportsSplitSmartspace,
            forceReloadBus,
            actionsFirst
        ) { sensitive, split, splitSupported, forceReloadTime, actionsFirst ->
            SessionSettings(
                sensitive,
                splitSupported && split,
                actionsFirst,
                forceReloadTime
            )
        }
    }

    private val smartspaceHolders by lazy {
        loadSmartspaceHolders()
    }

    private val smartspaceTargets by lazy {
        convert(smartspaceHolders, uiSurface)
    }

    private val targets by lazy {
        combine(targetCount, smartspaceTargets) { targetCount, targets ->
            targets.trimTo(targetCount)
        }
    }

    private val holders by lazy {
        combine(targetCount, smartspaceHolders) { targetCount, holders ->
            holders.trimPagesTo(targetCount)
        }.stateIn(lifecycleScope, SharingStarted.Eagerly, emptyList())
    }

    protected fun onCreate() {
        dispatcher.handleLifecycleEventSafely(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    open fun onResume() {
        if(lifecycle.currentState == Lifecycle.State.RESUMED) return
        dispatcher.handleLifecycleEventSafely(Lifecycle.Event.ON_RESUME)
        smartspaceRepository.setSmartspaceVisible(smartspaceSessionId, true)
        whenResumed {
            isVisible.emit(true)
        }
    }

    @CallSuper
    open fun onPause() {
        if(lifecycle.currentState != Lifecycle.State.RESUMED) return
        dispatcher.handleLifecycleEventSafely(Lifecycle.Event.ON_PAUSE)
        smartspaceRepository.setSmartspaceVisible(smartspaceSessionId, false)
        lifecycleScope.launch {
            isVisible.emit(false)
        }
    }

    @CallSuper
    open fun onDestroy() {
        whenCreated {
            dispatcher.handleLifecycleEventSafely(Lifecycle.Event.ON_DESTROY)
        }
        smartspaceRepository.onSessionDestroyed(smartspaceSessionId)
    }

    open fun Flow<List<T>>.filterDistinct(): Flow<List<T>> {
        return distinctUntilChanged { old, new -> old.deepEquals(new) }
    }

    open fun doesHaveSplitSmartspace() = false

    fun requestSmartspaceUpdate() {
        smartspaceRepository.requestSmartspaceUpdate(holders.value)
    }

    fun notifySmartspaceEvent(event: SmartspaceTargetEvent) {
        lifecycleScope.launch(Dispatchers.Main) {
            when (event.eventType) {
                SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN -> {
                    onResume()
                }
                SmartspaceTargetEvent.EVENT_UI_SURFACE_HIDDEN -> {
                    onPause()
                }
                SmartspaceTargetEvent.EVENT_TARGET_INTERACTION -> {
                    onInteraction(
                        event.smartspaceTarget ?: return@launch, event.smartspaceActionId
                    )
                }
                SmartspaceTargetEvent.EVENT_TARGET_DISMISS -> {
                    onDismiss(event.smartspaceTarget ?: return@launch)
                }
            }
        }
    }

    private fun onInteraction(target: SmartspaceTarget, actionId: String?) {
        val targetId = target.smartspaceTargetId
        smartspaceRepository.notifyClickEvent(targetId, actionId)
    }

    private fun onDismiss(target: SmartspaceTarget) {
        val targetId = target.smartspaceTargetId
        smartspaceRepository.notifyDismissEvent(targetId)
    }

    private fun setupTargetCollection() = whenCreated {
        //We apply a small debounce here to prevent jitter
        targets.filterDistinct().debounce(50L).collectLatest {
            collectInto(sessionId, it)
        }
    }

    private fun setupVisibleUpdateCheck() = whenCreated {
        //Check for update calls when Smartspace becomes visible, but ignore the init
        isVisible.filter { it }.drop(1).collectLatest {
            requestSmartspaceUpdate()
        }
    }

    private fun setupPeriodicUpdates() {
        //If periodic updates are enabled, make a request every 60 seconds
        if(!enablePeriodicUpdates) return
        whenCreated {
            while(dispatcher.currentState.isAtLeast(Lifecycle.State.CREATED)){
                delay(60_000L)
                requestSmartspaceUpdate()
            }
        }
    }

    private fun loadSmartspaceHolders(): Flow<List<SmartspacePageHolder>> {
        return combine(
            applyActionOverrides(filterTargets(smartspaceRepository.targets)),
            smartspaceRepository.actions,
            aodAudio,
            sessionSettings,
            uiSurface
        ) { t, a, aod, settings, surface ->
            val aodAudio = if(surface == UiSurface.LOCKSCREEN) aod else false
            val openMode = expandedOpenMode.firstNotNull()
            smartspaceRepository.mergeTargetsAndActions(
                t.filterTargets(surface, aodAudio)
                    .applySensitivity(settings.hideSensitive, surface)
                    .filterLimitedTargetSurfaces(surface),
                a.filterActions(surface, aodAudio)
                    .filterLimitedActionSurfaces(surface),
                openMode,
                surface,
                doesHaveSplitSmartspace() && settings.useSplitSmartspace,
                this is SystemSmartspacerSession,
                settings.actionsFirst
            )
        }
    }

    abstract val targetCount: Flow<Int>
    abstract suspend fun collectInto(id: I, targets: List<T>)
    abstract fun toSmartspacerSessionId(id: I): SmartspaceSessionId
    abstract fun convert(pages: Flow<List<SmartspacePageHolder>>, uiSurface: Flow<UiSurface>): Flow<List<T>>

    open fun filterTargets(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return targets
    }

    open fun applyActionOverrides(targets: Flow<List<TargetHolder>>): Flow<List<TargetHolder>> {
        return combine(
            expandedOpenMode.filterNotNull(),
            targets,
            uiSurface
        ) { openMode, t, surface ->
            if(openMode != ExpandedOpenMode.NEVER){
                t.applyOpenMode(openMode, surface)
            }else t
        }
    }

    private fun List<TargetHolder>.applyOpenMode(
        openMode: ExpandedOpenMode,
        surface: UiSurface
    ): List<TargetHolder> {
        return map { it.applyOpenMode(openMode, surface) }
    }

    private fun TargetHolder.applyOpenMode(
        openMode: ExpandedOpenMode,
        surface: UiSurface
    ): TargetHolder {
        return copy(targets = targets?.map { it.applyOpenMode(parent, openMode, surface) })
    }

    private fun SmartspaceTarget.applyOpenMode(
        parent: Target,
        openMode: ExpandedOpenMode,
        surface: UiSurface
    ): SmartspaceTarget {
        val config = parent.config
        val expanded = expandedState
        val shouldReplace = when {
            openMode == ExpandedOpenMode.ALWAYS -> true
            expanded == null -> false
            !config.showOnExpanded -> false
            !config.expandedShowWhenLocked && surface == UiSurface.LOCKSCREEN -> false
            expanded.widget != null && config.showWidget -> true
            expanded.shortcuts != null && config.showShortcuts -> true
            expanded.appShortcuts != null && config.showAppShortcuts -> true
            expanded.remoteViews != null && config.showRemoteViews -> true
            else -> false
        }
        return if(shouldReplace){
            replaceActionsWithExpanded(parent)
        }else this
    }

    private fun List<TargetHolder>.filterTargets(
        uiSurface: UiSurface,
        aodAudio: Boolean
    ): List<TargetHolder> {
        val isExpanded = this@BaseSmartspacerSession is ExpandedSmartspacerSession
        return filter {
            if(isExpanded) {
                when(uiSurface) {
                    UiSurface.HOMESCREEN -> {
                        it.parent.config.showOnExpanded
                    }
                    UiSurface.LOCKSCREEN -> {
                        it.parent.config.showOnExpanded && it.parent.config.expandedShowWhenLocked
                    }
                    UiSurface.MEDIA_DATA_MANAGER, UiSurface.GLANCEABLE_HUB -> {
                        false
                    }
                }
            }else{
                when (uiSurface) {
                    UiSurface.HOMESCREEN -> {
                        it.parent.config.showOnHomeScreen
                    }
                    UiSurface.LOCKSCREEN -> {
                        if(aodAudio){
                            it.parent.config.showOverMusic
                        }else {
                            it.parent.config.showOnLockScreen
                        }
                    }
                    UiSurface.MEDIA_DATA_MANAGER, UiSurface.GLANCEABLE_HUB -> {
                        false
                    }
                }
            }
        }
    }

    /**
     *  Applies [SmartspaceTarget.applySensitivity] to targets
     */
    private fun List<TargetHolder>.applySensitivity(
        hideSensitive: HideSensitive,
        uiSurface: UiSurface
    ): List<TargetHolder> {
        if(uiSurface != UiSurface.LOCKSCREEN) return this
        return mapNotNull {
            val filteredTargets = it.targets?.mapNotNull { target ->
                target.applySensitivity(hideSensitive)
            } ?: emptyList()
            if(filteredTargets.isEmpty()) return@mapNotNull null
            it.copy(targets = filteredTargets)
        }
    }

    /**
     *  Filters out Targets which have been developer-specified to not show on this surface. This
     *  is done after the user-specified option as it takes a higher priority
     */
    private fun List<TargetHolder>.filterLimitedTargetSurfaces(
        uiSurface: UiSurface
    ): List<TargetHolder> {
        return mapNotNull {
            val filteredTargets = it.targets?.filter { target ->
                target.shouldShowOnSurface(uiSurface)
            } ?: emptyList()
            if(filteredTargets.isEmpty()) return@mapNotNull null
            it.copy(targets = filteredTargets)
        }
    }

    /**
     *  Filters out Actions which have been developer-specified to not show on this surface. This
     *  is done after the user-specified option as it takes a higher priority
     */
    private fun List<ActionHolder>.filterLimitedActionSurfaces(
        uiSurface: UiSurface
    ): List<ActionHolder> {
        return mapNotNull {
            val filteredActions = it.actions?.filter { target ->
                target.shouldShowOnSurface(uiSurface)
            } ?: emptyList()
            if(filteredActions.isEmpty()) return@mapNotNull null
            it.copy(actions = filteredActions)
        }
    }

    /**
     *  If a [SmartspaceTarget] is marked as sensitive, strips its title and extra features, leaving
     *  the subtitle and icon intact. Does not remove the feature of weather targets to leave the
     *  date intact.
     */
    private fun SmartspaceTarget.applySensitivity(hideSensitive: HideSensitive): SmartspaceTarget? {
        if(!isSensitive) return this
        val shouldHideContents = when(hideSensitive) {
            HideSensitive.HIDE_CONTENTS -> true
            HideSensitive.DISABLED -> false
            else -> return null
        }
        val featureType = if(shouldHideContents && featureType != SmartspaceTarget.FEATURE_WEATHER){
            SmartspaceTarget.FEATURE_UNDEFINED
        }else{
            featureType
        }
        val headerAction = headerAction?.let { action ->
            if(!shouldHideContents) return@let action
            SmartspaceAction(
                action.id,
                action.icon,
                contentHiddenString,
                action.subtitle,
                contentHiddenString,
                action.pendingIntent,
                action.intent,
                action.userHandle,
                action.extras
            )
        }
        val templateData = templateData?.let {
            if(!shouldHideContents) return@let it
            BaseTemplateData(
                SmartspaceTarget.UI_TEMPLATE_DEFAULT,
                it.layoutWeight,
                it.primaryItem,
                it.subtitleItem,
                it.subtitleSupplementalItem,
                it.supplementalAlarmItem,
                it.supplementalLineItem
            ).also { template ->
                val primaryItem = template.primaryItem ?: return@also
                val text = primaryItem.text ?: return@also
                val newPrimaryItem = BaseTemplateData.SubItemInfo(primaryItem)
                val newText = Text(contentHiddenString, text.truncateAtType, text.maxLines)
                newPrimaryItem.text = newText
                template.primaryItem = newPrimaryItem
            }
        }
        return copy(
            headerAction = headerAction,
            featureType = featureType,
            templateData = templateData
        )
    }

    private fun List<ActionHolder>.filterActions(
        uiSurface: UiSurface,
        aodAudio: Boolean
    ): List<ActionHolder> {
        val isExpanded = this@BaseSmartspacerSession is ExpandedSmartspacerSession
        return filter {
            if(isExpanded) {
                when (uiSurface) {
                    UiSurface.HOMESCREEN -> {
                        it.parent.config.showOnExpanded
                    }
                    UiSurface.LOCKSCREEN -> {
                        it.parent.config.showOnExpanded && it.parent.config.expandedShowWhenLocked
                    }
                    UiSurface.MEDIA_DATA_MANAGER, UiSurface.GLANCEABLE_HUB -> {
                        false
                    }
                }
            }else{
                when (uiSurface) {
                    UiSurface.HOMESCREEN -> {
                        it.parent.config.showOnHomeScreen
                    }
                    UiSurface.LOCKSCREEN -> {
                        if (aodAudio) {
                            it.parent.config.showOverMusic
                        } else {
                            it.parent.config.showOnLockScreen
                        }
                    }
                    UiSurface.MEDIA_DATA_MANAGER, UiSurface.GLANCEABLE_HUB -> {
                        false
                    }
                }
            }
        }
    }

    open fun List<T>.trimTo(count: Int): List<T> {
        return take(count)
    }

    open fun List<SmartspacePageHolder>.trimPagesTo(count: Int): List<SmartspacePageHolder> {
        return take(count)
    }

    fun forceReload() = whenCreated {
        forceReloadBus.emit(System.currentTimeMillis())
    }

    init {
        setupTargetCollection()
        setupVisibleUpdateCheck()
        setupPeriodicUpdates()
    }

    data class SessionSettings(
        val hideSensitive: HideSensitive,
        val useSplitSmartspace: Boolean,
        val actionsFirst: Boolean,
        val forceReloadAt: Long
    )

}