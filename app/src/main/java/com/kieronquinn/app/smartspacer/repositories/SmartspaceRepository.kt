package com.kieronquinn.app.smartspacer.repositories

import android.content.Context
import com.kieronquinn.app.smartspacer.components.smartspace.compat.TargetMergerRegular
import com.kieronquinn.app.smartspacer.components.smartspace.compat.TargetMergerSplit
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DefaultComplication
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.model.smartspace.ActionHolder
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.model.smartspace.TargetHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspaceRepository.SmartspacePageHolder
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.ExpandedOpenMode
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerVisibilityChangedReceiver
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.fixActionsIfNeeded
import com.kieronquinn.app.smartspacer.utils.extensions.stripSmartspacerUniqueness
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.VisibleForTesting
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SmartspaceRepository {

    /**
     *  The current Target list
     */
    val targets: StateFlow<List<TargetHolder>>

    /**
     *  The current Complication list
     */
    val actions: StateFlow<List<ActionHolder>>

    /**
     *  Merges the given list of [targets] and [actions] into a list of [SmartspacePageHolder]
     *  pages
     */
    fun mergeTargetsAndActions(
        targets: List<TargetHolder>,
        actions: List<ActionHolder>,
        openMode: ExpandedOpenMode,
        surface: UiSurface,
        doesHaveSplitSmartspace: Boolean,
        isNative: Boolean,
        actionsFirst: Boolean
    ): List<SmartspacePageHolder>

    /**
     *  Set the visibility of a given smartspace session ID
     */
    fun setSmartspaceVisible(sessionId: SmartspaceSessionId, visible: Boolean)

    /**
     *  Clears the visibility state of a session when it is destroyed
     */
    fun onSessionDestroyed(sessionId: SmartspaceSessionId)

    /**
     *  Notifies a target and action with given IDs that a click has occurred
     */
    fun notifyClickEvent(targetId: String, actionId: String?)

    /**
     *  Notifies a target with a given ID that a dismiss click has occurred
     */
    fun notifyDismissEvent(targetId: String)

    /**
     *  Gets the current default home targets from a proxy Smartspace Session
     */
    fun getDefaultHomeTargets(): StateFlow<List<SmartspaceTarget>>

    /**
     *  Gets the current default lock targets from a proxy Smartspace Session
     */
    fun getDefaultLockTargets(): StateFlow<List<SmartspaceTarget>>

    /**
     *  Gets the current default home actions from a proxy Smartspace Session
     */
    fun getDefaultHomeActions(): StateFlow<List<SmartspaceAction>>

    /**
     *  Gets the current default lock actions from a proxy Smartspace Session
     */
    fun getDefaultLockActions(): StateFlow<List<SmartspaceAction>>

    /**
     *  Dismisses a default target of a given [targetId], which will notify changes back to the
     *  service
     */
    fun dismissDefaultTarget(targetId: String)

    /**
     *  Requests updates from all holders, except default targets (since the service is disconnected)
     */
    fun requestSmartspaceUpdate(holders: List<SmartspacePageHolder>)

    /**
     *  Requests updates for a given set of pages' actions and targets.
     *
     *  [limitToPackage] will only consider targets & actions belonging to a given package.
     */
    fun requestPluginUpdates(
        holders: List<SmartspacePageHolder>,
        limitToPackage: String? = null
    )

    data class SmartspacePageHolder(
        val page: SmartspaceTarget,
        val target: Target?,
        val actions: List<Action>
    )

    data class SmartspaceTargetHolder(
        val target: SmartspaceTarget,
        val parent: Target
    )

    data class SmartspaceActionHolder(
        val action: SmartspaceAction,
        val parent: Action
    )

}

class SmartspaceRepositoryImpl(
    private val context: Context,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    private val systemSmartspaceRepository: SystemSmartspaceRepository,
    targetsRepository: TargetsRepository,
    private val scope: CoroutineScope = MainScope(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): SmartspaceRepository, KoinComponent {

    companion object {
        private const val REFRESH_RATE_BUFFER = 5_000L // Short buffer for overlap
    }

    private val targetsRepository by inject<TargetsRepository>()
    private val updateLock = Mutex()

    @VisibleForTesting
    val smartspaceVisible = HashMap<String, Boolean>()

    @VisibleForTesting
    val lastPluginUpdateTimes = HashMap<String, Long>()

    private val availableTargets = targetsRepository.getAvailableTargets()
    private val availableComplications = targetsRepository.getAvailableComplications()

    private val _defaultHomeTargets = systemSmartspaceRepository.homeTargets.mapLatest {
        it.applyToTargets()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _defaultHomeActions = systemSmartspaceRepository.homeTargets.mapLatest {
        it.applyToActions()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _defaultLockTargets = systemSmartspaceRepository.lockTargets.mapLatest {
        it.applyToTargets()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _defaultLockActions = systemSmartspaceRepository.lockTargets.mapLatest {
        it.applyToActions()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val targets = availableTargets.flatMapLatest {
        mergeTargets(it)
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val actions = availableComplications.flatMapLatest {
        mergeActions(it)
    }.flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun setSmartspaceVisible(sessionId: SmartspaceSessionId, visible: Boolean) {
        smartspaceVisible[sessionId.id] = visible
        notifySessionVisibilityChanged()
    }

    override fun onSessionDestroyed(sessionId: SmartspaceSessionId) {
        smartspaceVisible.remove(sessionId.id)
        notifySessionVisibilityChanged()
    }

    private fun notifySessionVisibilityChanged() {
        targetsRepository.setSmartspaceVisibility(smartspaceVisible.values.any { it })
    }

    override fun notifyClickEvent(targetId: String, actionId: String?) {
        //Recreate the FLASHLIGHT action as it is broken by the mod.
        //Only default targets keep their default IDs, so this is guaranteed to come from AS
        if(targetId.startsWith("ambient_light") && actionId == "FLASHLIGHT") {
            toggleTorch()
            return
        }
    }

    override fun notifyDismissEvent(targetId: String) {
        val strippedTargetId = stripSmartspacerUniqueness(targetId)
        val target = targets.value.firstNotNullOfOrNull {
            if(it.targets?.any { target -> target.smartspaceTargetId == strippedTargetId } == true) {
                it.parent
            } else null
        }
        scope.launch {
            target?.onDismiss(strippedTargetId)
        }
    }

    override fun requestSmartspaceUpdate(holders: List<SmartspacePageHolder>) {
        scope.launch(dispatcher) {
            val packages = holders.flatMap { page ->
                listOfNotNull(
                    page.target?.sourcePackage,
                    *page.actions.map { it.sourcePackage }.toTypedArray()
                )
            }
            packages.distinct().forEach {
                SmartspacerUpdateReceiver.sendUpdateBroadcast(context, it)
                requestPluginUpdates(holders, it)
            }
        }
    }

    override fun requestPluginUpdates(
        holders: List<SmartspacePageHolder>,
        limitToPackage: String?
    ) {
        scope.launch(dispatcher) {
            updateLock.withLock {
                val targetUpdateCandidates = HashSet<UpdateCandidate>()
                holders.forEach { holder ->
                    holder.target?.let { target ->
                        val id = target.id ?: return@let
                        if (limitToPackage != null && target.sourcePackage != limitToPackage) return@let
                        val refreshPeriod = target.getPluginConfig().firstNotNull()
                            .refreshPeriodMinutes.toMilliseconds()
                        targetUpdateCandidates.add(
                            UpdateCandidate(
                                target.sourcePackage,
                                id,
                                target.authority,
                                refreshPeriod
                            )
                        )
                    }
                }
                //Add the Targets which have specified to always be updated
                targetUpdateCandidates.addAll(getAlwaysRefreshTargets())
                val targetsToUpdate = targetUpdateCandidates.filter {
                    val now = System.currentTimeMillis()
                    val last = lastPluginUpdateTimes[it.providerId] ?: 0L
                    it.refreshPeriod != 0L && now - last >= it.refreshPeriod - REFRESH_RATE_BUFFER
                }.groupBy { it.packageName }
                targetsToUpdate.forEach { pkg ->
                    val ids = pkg.value.map { it.providerId }.toTypedArray()
                    val authorities = pkg.value.map { it.authority }.toTypedArray()
                    val now = System.currentTimeMillis()
                    ids.forEach {
                        lastPluginUpdateTimes[it] = now
                    }
                    SmartspacerTargetUpdateReceiver.sendUpdateBroadcast(
                        context, pkg.key, ids, authorities
                    )
                }
                val actionUpdateCandidates = HashSet<UpdateCandidate>()
                holders.forEach { holder ->
                    holder.actions.forEach { action ->
                        val id = action.id ?: return@forEach
                        if (limitToPackage != null && action.sourcePackage != limitToPackage)
                            return@forEach
                        val refreshPeriod = action.getPluginConfig().firstNotNull()
                            .refreshPeriodMinutes.toMilliseconds()
                        actionUpdateCandidates.add(
                            UpdateCandidate(
                                action.sourcePackage,
                                id,
                                action.authority,
                                refreshPeriod
                            )
                        )
                    }
                }
                //Add the Actions which have specified to always be updated
                actionUpdateCandidates.addAll(getAlwaysRefreshActions())
                val actionsToUpdate = actionUpdateCandidates.filter {
                    val now = System.currentTimeMillis()
                    val last = lastPluginUpdateTimes[it.providerId] ?: 0L
                    it.refreshPeriod != 0L && now - last >= it.refreshPeriod - REFRESH_RATE_BUFFER
                }.groupBy { it.packageName }
                actionsToUpdate.forEach { pkg ->
                    val ids = pkg.value.map { it.providerId }.toTypedArray()
                    val authorities = pkg.value.map { it.authority }.toTypedArray()
                    val now = System.currentTimeMillis()
                    ids.forEach {
                        lastPluginUpdateTimes[it] = now
                    }
                    SmartspacerComplicationUpdateReceiver.sendUpdateBroadcast(
                        context, pkg.key, ids, authorities
                    )
                }
            }
        }
    }

    private suspend fun getAlwaysRefreshTargets(): List<UpdateCandidate> {
        return targets.value.mapNotNull {
            val config = it.parent.getPluginConfig().firstNotNull()
            if(!config.refreshIfNotVisible) return@mapNotNull null
            UpdateCandidate(
                it.parent.sourcePackage,
                it.parent.id ?: return@mapNotNull null,
                it.parent.authority,
                config.refreshPeriodMinutes.toMilliseconds()
            )
        }
    }

    private suspend fun getAlwaysRefreshActions(): List<UpdateCandidate> {
        return actions.value.mapNotNull {
            val config = it.parent.getPluginConfig().firstNotNull()
            if(!config.refreshIfNotVisible) return@mapNotNull null
            UpdateCandidate(
                it.parent.sourcePackage,
                it.parent.id ?: return@mapNotNull null,
                it.parent.authority,
                config.refreshPeriodMinutes.toMilliseconds()
            )
        }
    }

    data class UpdateCandidate(
        val packageName: String,
        val providerId: String,
        val authority: String,
        //Ignored in equals and hashCode as it can change but should not impact the Set
        val refreshPeriod: Long
    ) {

        override fun equals(other: Any?): Boolean {
            if(other !is UpdateCandidate) return false
            if(other.packageName != packageName) return false
            if(other.providerId != providerId) return false
            if(other.authority != authority) return false
            return true
        }

        override fun hashCode(): Int {
            var result = packageName.hashCode()
            result = 31 * result + providerId.hashCode()
            result = 31 * result + authority.hashCode()
            return result
        }

    }

    private fun Int.toMilliseconds(): Long {
        return 60_000L * this
    }

    override fun dismissDefaultTarget(targetId: String) {
        systemSmartspaceRepository.dismissDefaultTarget(targetId)
    }

    private fun mergeTargets(
        targets: List<Target>
    ): Flow<List<TargetHolder>> {
        if(targets.isEmpty()) return flowOf(emptyList())
        return combine(*targets.toTypedArray()) {
            it.toList()
        }
    }

    private fun mergeActions(
        actions: List<Action>
    ): Flow<List<ActionHolder>> {
        if(actions.isEmpty()) return flowOf(emptyList())
        return combine(*actions.toTypedArray()) {
            it.toList()
        }
    }

    override fun mergeTargetsAndActions(
        targets: List<TargetHolder>,
        actions: List<ActionHolder>,
        openMode: ExpandedOpenMode,
        surface: UiSurface,
        doesHaveSplitSmartspace: Boolean,
        isNative: Boolean,
        actionsFirst: Boolean
    ): List<SmartspacePageHolder> {
        return when {
            doesHaveSplitSmartspace && surface == UiSurface.LOCKSCREEN -> {
                TargetMergerSplit.mergeTargetsAndActions(
                    targets,
                    actions,
                    openMode,
                    actionsFirst
                )
            }
            else -> {
                TargetMergerRegular.mergeTargetsAndActions(
                    targets,
                    actions,
                    openMode,
                    actionsFirst
                )
            }
        }
    }

    override fun getDefaultHomeTargets(): StateFlow<List<SmartspaceTarget>> {
        return _defaultHomeTargets
    }

    override fun getDefaultHomeActions(): StateFlow<List<SmartspaceAction>> {
        return _defaultHomeActions
    }

    override fun getDefaultLockTargets(): StateFlow<List<SmartspaceTarget>> {
        return _defaultLockTargets
    }

    override fun getDefaultLockActions(): StateFlow<List<SmartspaceAction>> {
        return _defaultLockActions
    }

    private fun notifyDefaultHomeTargets() = scope.launch {
        _defaultHomeTargets.collect {
            SmartspacerTargetProvider.notifyChange(context, DefaultTarget::class.java)
        }
    }

    private fun notifyDefaultHomeActions() = scope.launch {
        _defaultHomeActions.collect {
            SmartspacerComplicationProvider.notifyChange(context, DefaultComplication::class.java)
        }
    }

    private fun notifyDefaultLockTargets() = scope.launch {
        _defaultLockTargets.collect {
            SmartspacerTargetProvider.notifyChange(context, DefaultTarget::class.java)
        }
    }

    private fun notifyDefaultLockActions() = scope.launch {
        _defaultLockActions.collect {
            SmartspacerComplicationProvider.notifyChange(context, DefaultComplication::class.java)
        }
    }

    private fun toggleTorch() = scope.launch {
        shizukuServiceRepository.runWithService { it.toggleTorch() }
    }

    /**
     *  Returns if the Target coming from ASI is of Weather type
     */
    private fun SmartspaceTarget.isWeatherComplication(): Boolean {
        return featureType == SmartspaceTarget.FEATURE_WEATHER
    }

    private fun List<SmartspaceTarget>.applyToTargets(): List<SmartspaceTarget> {
        return filterNot { target ->
            target.isWeatherComplication()
        }.map { target ->
            target.fixActionsIfNeeded(context)
        }
    }

    private fun List<SmartspaceTarget>.applyToActions(): List<SmartspaceAction> {
        return filter { target ->
            target.isWeatherComplication()
        }.map { target ->
            target.fixActionsIfNeeded(context)
        }.map { target ->
            val subtitle = target.templateData?.subtitleItem
            val supplementalSubtitle = target.templateData?.subtitleSupplementalItem
            val header = when {
                target.headerAction?.subtitle?.isNotBlank() == true -> target.headerAction
                subtitle?.text?.text?.isNotEmpty() == true -> {
                    subtitle.generateSmartspaceAction(target, true)
                }
                else -> null
            }
            val base = when {
                target.baseAction?.subtitle?.isNotBlank() == true -> target.baseAction
                supplementalSubtitle?.text?.text?.isNotEmpty() == true -> {
                    supplementalSubtitle.generateSmartspaceAction(target, false)
                }
                else -> null
            }
            listOfNotNull(header, base)
        }.flatten()
    }

    private fun setupSmartspaceVisibilityBroadcasts() = scope.launch {
        val packages = combine(
            availableTargets,
            availableComplications
        ) { targets, complications ->
            targets.map { it.sourcePackage } + complications.map { it.sourcePackage }
        }.stateIn(scope, SharingStarted.Eagerly, emptySet())
        val packagesToSend = suspend {
            (packages.value +
                    getAlwaysRefreshTargets().map { it.packageName } +
                    getAlwaysRefreshTargets().map { it.packageName }).distinct()
        }
        targetsRepository.smartspaceVisible.collect { visible ->
            val timestamp = System.currentTimeMillis()
            packagesToSend().forEach {
                SmartspacerVisibilityChangedReceiver.sendVisibilityChangedBroadcast(
                    context, visible, it, timestamp
                )
            }
        }
    }

    init {
        notifyDefaultHomeTargets()
        notifyDefaultHomeActions()
        notifyDefaultLockTargets()
        notifyDefaultLockActions()
        setupSmartspaceVisibilityBroadcasts()
    }

}