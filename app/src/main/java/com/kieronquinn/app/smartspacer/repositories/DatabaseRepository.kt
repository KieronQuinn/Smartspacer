package com.kieronquinn.app.smartspacer.repositories

import com.kieronquinn.app.smartspacer.model.database.Action
import com.kieronquinn.app.smartspacer.model.database.ActionData
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.model.database.BroadcastListener
import com.kieronquinn.app.smartspacer.model.database.ExpandedAppWidget
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.model.database.Grant
import com.kieronquinn.app.smartspacer.model.database.NotificationListener
import com.kieronquinn.app.smartspacer.model.database.Requirement
import com.kieronquinn.app.smartspacer.model.database.RequirementData
import com.kieronquinn.app.smartspacer.model.database.SmartspacerDatabase
import com.kieronquinn.app.smartspacer.model.database.Target
import com.kieronquinn.app.smartspacer.model.database.TargetData
import com.kieronquinn.app.smartspacer.model.database.Widget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface DatabaseRepository {

    fun getTargets(): Flow<List<Target>>
    fun getActions(): Flow<List<Action>>
    fun getNotificationListeners(): Flow<List<NotificationListener>>
    fun getBroadcastListeners(): Flow<List<BroadcastListener>>
    fun getWidgets(): Flow<List<Widget>>
    fun getAppWidgets(): Flow<List<AppWidget>>
    fun getGrants(): Flow<List<Grant>>
    fun getExpandedAppWidgets(): Flow<List<ExpandedAppWidget>>
    fun getExpandedCustomAppWidgets(): Flow<List<ExpandedCustomAppWidget>>
    fun getTargetData(): Flow<List<TargetData>>
    fun getActionData(): Flow<List<ActionData>>
    fun getRequirementData(): Flow<List<RequirementData>>
    fun getRequirements(): Flow<List<Requirement>>

    fun getTargetById(id: String): Flow<Target?>
    fun getActionById(id: String): Flow<Action?>
    fun getRequirementById(id: String): Flow<Requirement?>
    suspend fun getGrantForPackage(packageName: String): Grant?
    suspend fun getAppWidgetById(id: Int): AppWidget?

    suspend fun addAction(action: Action)
    suspend fun addAppWidget(appWidget: AppWidget)
    suspend fun addTarget(target: Target)
    suspend fun addNotificationListener(notificationListener: NotificationListener)
    suspend fun addBroadcastListener(broadcastListener: BroadcastListener)
    suspend fun addRequirement(requirement: Requirement)
    suspend fun addRequirementData(requirementData: RequirementData)
    suspend fun addTargetData(targetData: TargetData)
    suspend fun addActionData(actionData: ActionData)
    suspend fun addWidget(widget: Widget)
    suspend fun addGrant(grant: Grant)
    suspend fun addExpandedAppWidget(expandedAppWidget: ExpandedAppWidget)
    suspend fun addExpandedCustomAppWidget(expandedCustomAppWidget: ExpandedCustomAppWidget)

    suspend fun updateAction(action: Action)
    suspend fun updateTarget(target: Target)
    suspend fun updateRequirement(requirement: Requirement)
    suspend fun updateWidget(widget: Widget)
    suspend fun updateAppWidget(appWidget: AppWidget)
    suspend fun updateExpandedCustomAppWidget(expandedCustomAppWidget: ExpandedCustomAppWidget)

    suspend fun updateTargetConfig(id: String, configChange: (Target) -> Unit)
    suspend fun updateActionConfig(id: String, configChange: (Action) -> Unit)

    suspend fun deleteAction(action: Action)
    suspend fun deleteAppWidget(appWidget: AppWidget)
    suspend fun deleteTarget(target: Target)
    suspend fun deleteNotificationListener(id: String)
    suspend fun deleteBroadcastListener(id: String)
    suspend fun deleteRequirement(requirement: Requirement)
    suspend fun deleteWidget(id: String, type: Widget.Type)
    suspend fun deleteExpandedAppWidget(id: Int)
    suspend fun deleteExpandedCustomAppWidget(id: Int)
    fun deleteRequirementData(id: String)
    fun deleteTargetData(id: String)
    fun deleteActionData(id: String)

}

class DatabaseRepositoryImpl(
    database: SmartspacerDatabase
): DatabaseRepository {

    private val scope = MainScope()

    private val actions = database.actionDao()
    private val targets = database.targetDao()
    private val grant = database.grantDao()
    private val notificationListeners = database.notificationListenerDao()
    private val broadcastListeners = database.broadcastListenerDao()
    private val requirements = database.requirementDao()
    private val requirementData = database.requirementDataDao()
    private val actionData = database.actionDataDao()
    private val targetData = database.targetDataDao()
    private val widgets = database.widgetDao()
    private val appWidgets = database.appWidgetDao()
    private val expandedAppWidget = database.expandedAppWidgetDao()
    private val expandedCustomAppWidget = database.expandedCustomAppWidgetDao()

    override fun getActions() = actions.getAll()
    override fun getTargets() = targets.getAll()
    override fun getNotificationListeners() = notificationListeners.getAll()
    override fun getBroadcastListeners() = broadcastListeners.getAll()
    override fun getWidgets() = widgets.getAll()
    override fun getExpandedAppWidgets() = expandedAppWidget.getAll()
    override fun getExpandedCustomAppWidgets() = expandedCustomAppWidget.getAll()

    override suspend fun getGrantForPackage(packageName: String): Grant? {
        return withContext(Dispatchers.IO){
            grant.getGrantForPackage(packageName)
        }
    }

    override fun getTargetById(id: String): Flow<Target?> {
        return targets.getTarget(id)
    }

    override fun getActionById(id: String): Flow<Action?> {
        return actions.getAction(id)
    }

    override fun getAppWidgets(): Flow<List<AppWidget>> {
        return appWidgets.getAll()
    }

    override fun getGrants(): Flow<List<Grant>> {
        return grant.getAll()
    }

    override fun getRequirements(): Flow<List<Requirement>> {
        return requirements.getAll()
    }

    override fun getTargetData(): Flow<List<TargetData>> {
        return targetData.getAll()
    }

    override fun getActionData(): Flow<List<ActionData>> {
        return actionData.getAll()
    }

    override fun getRequirementData(): Flow<List<RequirementData>> {
        return requirementData.getAll()
    }

    override fun getRequirementById(id: String): Flow<Requirement?> {
        return requirements.getById(id)
    }

    override suspend fun getAppWidgetById(id: Int): AppWidget? = withContext(Dispatchers.IO) {
        appWidgets.getById(id)
    }

    override suspend fun addAction(action: Action) = withContext(Dispatchers.IO) {
        actions.insert(action)
    }

    override suspend fun addTarget(target: Target) = withContext(Dispatchers.IO) {
        targets.insert(target)
    }

    override suspend fun addGrant(grant: Grant) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.grant.addGrant(grant)
    }

    override suspend fun addNotificationListener(notificationListener: NotificationListener) {
        withContext(Dispatchers.IO){
            notificationListeners.insert(notificationListener)
        }
    }

    override suspend fun addBroadcastListener(broadcastListener: BroadcastListener) {
        withContext(Dispatchers.IO){
            broadcastListeners.insert(broadcastListener)
        }
    }

    override suspend fun addRequirement(requirement: Requirement) = withContext(Dispatchers.IO) {
        requirements.insert(requirement)
    }

    override suspend fun addRequirementData(requirementData: RequirementData) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.requirementData.insert(requirementData)
    }

    override suspend fun addActionData(actionData: ActionData) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.actionData.insert(actionData)
    }

    override suspend fun addTargetData(targetData: TargetData) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.targetData.insert(targetData)
    }

    override suspend fun addWidget(widget: Widget) = withContext(Dispatchers.IO) {
        widgets.insert(widget)
    }

    override suspend fun addAppWidget(appWidget: AppWidget) = withContext(Dispatchers.IO) {
        appWidgets.insert(appWidget)
    }

    override suspend fun addExpandedAppWidget(expandedAppWidget: ExpandedAppWidget) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.expandedAppWidget.setExpandedAppWidget(expandedAppWidget)
    }

    override suspend fun addExpandedCustomAppWidget(
        expandedCustomAppWidget: ExpandedCustomAppWidget
    ) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.expandedCustomAppWidget.insert(expandedCustomAppWidget)
    }

    override suspend fun updateAction(action: Action) = withContext(Dispatchers.IO) {
        actions.update(action)
    }

    override suspend fun updateTarget(target: Target) = withContext(Dispatchers.IO) {
        targets.update(target)
    }

    override suspend fun updateRequirement(requirement: Requirement) = withContext(Dispatchers.IO) {
        requirements.update(requirement)
    }

    override suspend fun updateTargetConfig(id: String, configChange: (Target) -> Unit) {
        withContext(Dispatchers.IO){
            val target = targets.getTarget(id).first() ?: return@withContext
            configChange(target)
            updateTarget(target)
        }
    }

    override suspend fun updateActionConfig(id: String, configChange: (Action) -> Unit) {
        withContext(Dispatchers.IO){
            val action = actions.getAction(id).first() ?: return@withContext
            configChange(action)
            updateAction(action)
        }
    }

    override suspend fun updateAppWidget(appWidget: AppWidget) = withContext(Dispatchers.IO) {
        appWidgets.update(appWidget)
    }

    override suspend fun updateExpandedCustomAppWidget(
        expandedCustomAppWidget: ExpandedCustomAppWidget
    ) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.expandedCustomAppWidget.update(expandedCustomAppWidget)
    }

    override suspend fun updateWidget(widget: Widget) = withContext(Dispatchers.IO) {
        widgets.update(widget)
    }

    override suspend fun deleteAction(action: Action) = withContext(Dispatchers.IO) {
        actions.delete(action)
    }

    override suspend fun deleteTarget(target: Target) = withContext(Dispatchers.IO) {
        targets.delete(target)
    }

    override suspend fun deleteNotificationListener(id: String) {
        withContext(Dispatchers.IO){
            notificationListeners.delete(id)
        }
    }

    override suspend fun deleteBroadcastListener(id: String) {
        withContext(Dispatchers.IO){
            broadcastListeners.delete(id)
        }
    }

    override suspend fun deleteRequirement(requirement: Requirement) = withContext(Dispatchers.IO) {
        requirements.delete(requirement)
    }

    override fun deleteRequirementData(id: String) {
        scope.launch(Dispatchers.IO) {
            requirementData.getById(id)?.let {
                requirementData.delete(it)
            }
        }
    }

    override fun deleteTargetData(id: String) {
        scope.launch(Dispatchers.IO) {
            targetData.getById(id)?.let {
                targetData.delete(it)
            }
        }
    }

    override fun deleteActionData(id: String) {
        scope.launch(Dispatchers.IO) {
            actionData.getById(id)?.let {
                actionData.delete(it)
            }
        }
    }

    override suspend fun deleteWidget(id: String, type: Widget.Type) {
        withContext(Dispatchers.IO) {
            scope.launch(Dispatchers.IO) {
                widgets.getWidget(id, type).first()?.let {
                    widgets.delete(it)
                }
            }
        }
    }

    override suspend fun deleteAppWidget(appWidget: AppWidget) = withContext(Dispatchers.IO) {
        appWidgets.delete(appWidget)
    }

    override suspend fun deleteExpandedAppWidget(id: Int) = withContext(Dispatchers.IO) {
        expandedAppWidget.delete(id)
    }

    override suspend fun deleteExpandedCustomAppWidget(id: Int) = withContext(Dispatchers.IO) {
        this@DatabaseRepositoryImpl.expandedCustomAppWidget.delete(id)
    }

}