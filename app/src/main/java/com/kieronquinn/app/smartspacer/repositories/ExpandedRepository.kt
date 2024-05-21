package com.kieronquinn.app.smartspacer.repositories

import android.app.prediction.AppTarget
import android.app.prediction.AppTargetEvent
import android.app.prediction.AppTargetId
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import android.content.pm.ParceledListSlice
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.Process
import android.util.Log
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.IAppPredictionOnTargetsAvailableListener
import com.kieronquinn.app.smartspacer.ISmartspacerShizukuService
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.model.database.ExpandedAppWidget
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.ExpandedCustomWidgetBackup
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSession
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedSessionImpl
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.getHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetColumnWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getWidgetRowHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidth
import com.kieronquinn.app.smartspacer.utils.extensions.px
import com.kieronquinn.app.smartspacer.utils.remoteviews.WidgetContextWrapper
import com.kieronquinn.app.smartspacer.widget.ExpandedAppWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

interface ExpandedRepository {

    /**
     *  Emits when the ExpandedActivity detects a back press from the overlay
     */
    val overlayBackPressedBus: Flow<Unit>

    /**
     *  Emits when the overlay is dragged
     */
    val overlayDragProgressChanged: Flow<Unit>

    /**
     *  The current list of custom app widgets to show at the bottom of the expanded screen
     */
    val expandedCustomAppWidgets: Flow<List<ExpandedCustomAppWidget>>

    /**
     *  Whether to force the use of Google Sans in the widgets on the Expanded Screen
     */
    val widgetUseGoogleSans: Boolean

    /**
     *  Gets stored [ExpandedAppWidget]s, which map [AppWidgetProviderInfo] components to IDs
     */
    fun getExpandedAppWidgets(): Flow<List<ExpandedAppWidget>>

    /**
     *  Saves an [AppWidgetProviderInfo]'s App Widget ID, mapping to the provider and ID. Using the
     *  same ID and provider (or no ID and the same provider) will re-use a widget.
     */
    suspend fun commitExpandedAppWidget(
        provider: AppWidgetProviderInfo,
        appWidgetId: Int,
        id: String?,
        customConfig: CustomExpandedAppWidgetConfig?
    )

    /**
     *  Allocate an App Widget ID from the expanded host
     */
    fun allocateAppWidgetId(): Int

    /**
     *  Deallocate an App Widget ID from the expanded host
     */
    fun deallocateAppWidgetId(id: Int)

    /**
     *  Removes an app widget from the database and deallocates its ID
     */
    suspend fun removeAppWidget(appWidgetId: Int)

    /**
     *  Removes an app widget from the database and deallocates its ID
     */
    suspend fun removeAppWidget(widgetId: String)

    /**
     *  Binds an app widget [provider] to an [id], if possible
     *
     *  @return Whether the bind was successful, if not then the system dialog should be shown.
     */
    fun bindAppWidgetIdIfAllowed(id: Int, provider: ComponentName): Boolean

    /**
     *  Creates an [IntentSender] for (re)configuring this widget
     */
    fun createConfigIntentSender(appWidgetId: Int): IntentSender

    /**
     *  Creates an [AppWidgetHostView] for a given [widget] and [sessionId]
     */
    fun createHost(
        context: Context,
        availableWidth: Int,
        widget: Item.Widget,
        sessionId: String,
        handler: SmartspacerBasePageView.SmartspaceTargetInteractionListener
    ): ExpandedAppWidgetHostView

    /**
     *  Destroys the attached AppWidgetHostViews in the map for a given [sessionId]
     */
    fun destroyHosts(sessionId: String?)

    suspend fun onOverlayBackPressed()
    suspend fun onOverlayDragProgressChanged()

    suspend fun getExpandedCustomWidgetBackups(): List<ExpandedCustomWidgetBackup>
    suspend fun restoreExpandedCustomWidgetBackups(backups: List<ExpandedCustomWidgetBackup>)

    /**
     *  On devices where it is supported (Enhanced Mode + system support is required), returns
     *  a list of recommended widgets from the system predictor
     */
    suspend fun getPredictedWidgets(): List<AppWidgetProviderInfo>

    fun getExpandedSession(context: Context, sessionId: String): ExpandedSession
    fun destroyExpandedSession(sessionId: String)

    @Parcelize
    data class CustomExpandedAppWidgetConfig(
        val spanX: Int,
        val spanY: Int,
        val index: Int,
        val showWhenLocked: Boolean,
        val roundCorners: Boolean,
        val fullWidth: Boolean
    ): Parcelable

    @Parcelize
    data class ExpandedCustomWidgetBackup(
        @SerializedName("provider")
        val provider: String,
        @SerializedName("id")
        val id: String,
        @SerializedName("index")
        val index: Int,
        @SerializedName("span_x")
        val spanX: Int,
        @SerializedName("span_y")
        val spanY: Int,
        @SerializedName("show_when_locked")
        val showWhenLocked: Boolean,
        @SerializedName("round_corners")
        val roundCorners: Boolean? = null,
        @SerializedName("full_width")
        val fullWidth: Boolean? = null
    ): Parcelable

}

class ExpandedRepositoryImpl(
    context: Context,
    private val settings: SmartspacerSettingsRepository,
    private val databaseRepository: DatabaseRepository,
    private val widgetRepository: WidgetRepository,
    private val shizukuServiceRepository: ShizukuServiceRepository,
    scope: CoroutineScope = MainScope()
): ExpandedRepository {

    override val overlayBackPressedBus = MutableSharedFlow<Unit>()
    override val overlayDragProgressChanged = MutableSharedFlow<Unit>()
    override val expandedCustomAppWidgets = databaseRepository.getExpandedCustomAppWidgets()

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val widgetHostContext = WidgetContextWrapper(context)
    private var appWidgetHostViews = HashMap<CacheTag, ExpandedAppWidgetHostView>()
    private val expandedSessions = HashMap<String, ExpandedSession>()

    private val widgetsUseGoogleSans = settings.expandedWidgetUseGoogleSans.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, settings.expandedWidgetUseGoogleSans.getSync())

    @VisibleForTesting
    var appWidgetHost = ExpandedAppWidgetHost.create(widgetHostContext, 1).also {
        it.startListening()
    }

    override val widgetUseGoogleSans: Boolean
        get() = widgetsUseGoogleSans.value

    override fun getExpandedAppWidgets(): Flow<List<ExpandedAppWidget>> {
        return databaseRepository.getExpandedAppWidgets()
    }

    override suspend fun commitExpandedAppWidget(
        provider: AppWidgetProviderInfo,
        appWidgetId: Int,
        id: String?,
        customConfig: CustomExpandedAppWidgetConfig?
    ) {
        if(customConfig != null){
            val widget = ExpandedCustomAppWidget(
                id = id ?: UUID.randomUUID().toString(),
                appWidgetId = appWidgetId,
                provider = provider.provider.flattenToString(),
                index = customConfig.index,
                spanX = customConfig.spanX,
                spanY = customConfig.spanY,
                showWhenLocked = customConfig.showWhenLocked,
                roundCorners = customConfig.roundCorners,
                fullWidth = customConfig.fullWidth
            )
            databaseRepository.addExpandedCustomAppWidget(widget)
        }else {
            databaseRepository.addExpandedAppWidget(
                ExpandedAppWidget(appWidgetId, provider.provider.flattenToString(), id)
            )
        }
    }

    override suspend fun onOverlayBackPressed() {
        overlayBackPressedBus.emit(Unit)
    }

    override suspend fun onOverlayDragProgressChanged() {
        overlayDragProgressChanged.emit(Unit)
    }

    override suspend fun getExpandedCustomWidgetBackups(): List<ExpandedCustomWidgetBackup> {
        val widgets = databaseRepository.getExpandedCustomAppWidgets().first()
        return widgets.map {
            ExpandedCustomWidgetBackup(
                it.provider,
                it.id,
                it.index,
                it.spanX,
                it.spanY,
                it.showWhenLocked
            )
        }
    }

    override suspend fun restoreExpandedCustomWidgetBackups(
        backups: List<ExpandedCustomWidgetBackup>
    ) = withContext(Dispatchers.IO) {
        val current = databaseRepository.getExpandedCustomAppWidgets().first()
        current.forEach {
            databaseRepository.deleteExpandedCustomAppWidget(it.appWidgetId ?: return@forEach)
            widgetRepository.deallocateAppWidgetId(it.appWidgetId)
        }
        backups.forEach {
            //Make sure provider still exists
            val provider = ComponentName.unflattenFromString(it.provider) ?: return@forEach
            val exists = widgetRepository.getProviders().any { p -> p.provider == provider }
            if(!exists) return@forEach
            val widget = ExpandedCustomAppWidget(
                it.id,
                null,
                it.provider,
                it.index,
                it.spanX,
                it.spanY,
                it.showWhenLocked,
                it.roundCorners ?: true,
                it.fullWidth ?: false
            )
            databaseRepository.addExpandedCustomAppWidget(widget)
        }
    }

    override fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    override fun deallocateAppWidgetId(id: Int) {
        appWidgetHost.deleteAppWidgetId(id)
    }

    override suspend fun removeAppWidget(appWidgetId: Int) {
        databaseRepository.deleteExpandedAppWidget(appWidgetId)
        deallocateAppWidgetId(appWidgetId)
    }

    override suspend fun removeAppWidget(widgetId: String) {
        val id = databaseRepository.getExpandedAppWidgets().firstOrNull()?.firstOrNull {
            it.id == widgetId
        }?.appWidgetId ?: return
        removeAppWidget(id)
    }

    override fun bindAppWidgetIdIfAllowed(id: Int, provider: ComponentName): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(id, provider)
    }

    override fun createConfigIntentSender(appWidgetId: Int): IntentSender {
        return appWidgetHost.getIntentSenderForConfigureActivity(appWidgetId, 0)
    }

    override fun createHost(
        context: Context,
        availableWidth: Int,
        widget: Item.Widget,
        sessionId: String,
        handler: SmartspacerBasePageView.SmartspaceTargetInteractionListener
    ): ExpandedAppWidgetHostView {
        val appWidgetId = widget.appWidgetId
            ?: throw RuntimeException("Cannot create a widget without an ID!")
        val widgetContext = WidgetContextWrapper(context)
        val widgetColumnWidth = context.getWidgetColumnWidth(availableWidth)
        val widgetRowHeight = context.getWidgetRowHeight(availableWidth)
        val width = when {
            widget.width != null && widget.width != 0 -> widget.width.coerceAtMost(availableWidth)
            widget.spanX != null -> widget.spanX * widgetColumnWidth
            else -> widget.provider.getWidth(context, availableWidth, widgetColumnWidth)
        } - 16.dp
        val height = when {
            widget.height != null && widget.height != 0 -> widget.height
            widget.spanY != null -> widget.spanY * widgetRowHeight
            else -> widget.provider.getHeight(context, availableWidth, widgetRowHeight)
        }
        val cacheTag = CacheTag(appWidgetId, width, height, widget.useGoogleSans, sessionId)
        val widgetWidth = with(context.resources) {
            px(width).toFloat()
        }
        val widgetHeight = with(context.resources) {
            px(height).toFloat()
        }
        appWidgetHostViews.destroyStaleHosts(cacheTag)
        appWidgetHostViews[cacheTag]?.let {
            return it.also {
                it.updateSizeIfNeeded(widgetWidth, widgetHeight)
            }
        }
        return appWidgetHost.createView(
            widgetContext, appWidgetId, sessionId, widget.provider, handler
        ).apply {
            this as ExpandedAppWidgetHostView
            updateSizeIfNeeded(widgetWidth, widgetHeight)
            layoutParams = ViewGroup.LayoutParams(width, height)
            appWidgetHostViews[cacheTag] = this
        } as ExpandedAppWidgetHostView
    }

    override fun destroyHosts(sessionId: String?) = with(appWidgetHostViews) {
        val toDestroy = entries.filter { it.key.sessionId == sessionId }
        toDestroy.forEach {
            appWidgetHost.destroyView(it.value)
            remove(it.key)
        }
    }

    override suspend fun getPredictedWidgets(): List<AppWidgetProviderInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        val widgetFlow = shizukuServiceRepository.runWithService {
            it.getPredictedWidgets().map { predictions ->
                predictions.toWidgets()
            }
        }.unwrap() ?: return emptyList()
        return withTimeoutOrNull(2500L) {
            widgetFlow.firstOrNull() ?: emptyList()
        } ?: emptyList()
    }

    override fun getExpandedSession(context: Context, sessionId: String): ExpandedSession {
        synchronized(expandedSessions) {
            return expandedSessions[sessionId] ?: ExpandedSessionImpl(context, settings).also {
                expandedSessions[sessionId] = it
            }
        }
    }

    override fun destroyExpandedSession(sessionId: String) {
        synchronized(expandedSessions) {
            expandedSessions.remove(sessionId)?.also {
                it.onDestroy()
            }
        }
    }

    private fun ISmartspacerShizukuService.getPredictedWidgets() = callbackFlow {
        val listener = object : IAppPredictionOnTargetsAvailableListener.Stub() {
            override fun onTargetsAvailable(targets: ParceledListSlice<*>) {
                targets as ParceledListSlice<AppTarget>
                trySend(targets.list)
            }
        }
        createWidgetPredictorSession(listener, getWidgetsForPredictor())
        awaitClose {
            destroyWidgetPredictorSession()
        }
    }

    private suspend fun getWidgetsForPredictor(): Bundle {
        val providers = widgetRepository.getProviders()
        val widgets = expandedCustomAppWidgets.first().mapNotNull { widget ->
            providers.firstOrNull { info ->
                info.provider == ComponentName.unflattenFromString(widget.provider)
            }?.toAppTarget()?.wrapWithLocationInformation(widget.index, widget.spanX, widget.spanY)
        }
        return bundleOf(
            "added_app_widgets" to ArrayList(widgets)
        )
    }

    private fun AppWidgetProviderInfo.toAppTarget(): AppTarget {
        return AppTarget.Builder(
            AppTargetId("widget:${provider.packageName}"),
            provider.packageName,
            Process.myUserHandle()
        ).setClassName(provider.className).build()
    }

    private fun AppTarget.wrapWithLocationInformation(
        index: Int,
        spanX: Int,
        spanY: Int
    ): AppTargetEvent {
        return AppTargetEvent.Builder(this, AppTargetEvent.ACTION_PIN)
            //We don't have pages nor a [x,y] pos, so use the index as the page and lock at [0,0]
            .setLaunchLocation("workspace/$index/[0,0]/[$spanX,$spanY]").build()
    }

    private suspend fun List<AppTarget>.toWidgets(): List<AppWidgetProviderInfo> {
        val providers = widgetRepository.getProviders()
        val added = expandedCustomAppWidgets.first().mapNotNull {
            ComponentName.unflattenFromString(it.provider)
        }
        return mapNotNull {
            val componentName = ComponentName(
                it.packageName,
                it.className ?: return@mapNotNull null
            )
            providers.firstOrNull { widget -> widget.provider == componentName }
        }.filterNot {
            //Remove any which are already on Expanded
            added.any { widget -> widget == it.provider }
        }
    }

    /**
     *  When widget width/height values change, the same app widget ID and session ID will be
     *  sent, but the AppWidgetHosts will be stale and will need to be recreated. Therefore, we
     *  destroy the stale ones and remove them from the map. New views will be created instead.
     */
    private fun HashMap<CacheTag, ExpandedAppWidgetHostView>.destroyStaleHosts(
        cacheTag: CacheTag
    ) {
        val toDestroy = entries.filter {
            it.key.appWidgetId == cacheTag.appWidgetId
                    && it.key.sessionId == cacheTag.sessionId
                    && it.key.height != cacheTag.height
                    && it.key.width != cacheTag.width
                    && it.key.useGoogleSans != cacheTag.useGoogleSans
        }
        toDestroy.forEach {
            appWidgetHost.destroyView(it.value)
            remove(it.key)
        }
    }

    data class CacheTag(
        val appWidgetId: Int,
        val width: Int,
        val height: Int,
        val useGoogleSans: Boolean,
        val sessionId: String?
    )

}