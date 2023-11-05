package com.kieronquinn.app.smartspacer.repositories

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import android.os.Parcelable
import android.view.ViewGroup
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.ExpandedSmartspacerSession.Item
import com.kieronquinn.app.smartspacer.model.database.ExpandedAppWidget
import com.kieronquinn.app.smartspacer.model.database.ExpandedCustomAppWidget
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.CustomExpandedAppWidgetConfig
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository.ExpandedCustomWidgetBackup
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.ui.views.appwidget.ExpandedAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.getDisplayPortraitWidth
import com.kieronquinn.app.smartspacer.utils.extensions.getHeight
import com.kieronquinn.app.smartspacer.utils.extensions.getWidth
import com.kieronquinn.app.smartspacer.utils.remoteviews.WidgetContextWrapper
import com.kieronquinn.app.smartspacer.widget.ExpandedAppWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.jetbrains.annotations.VisibleForTesting
import java.util.*

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
     *  The width of a widget's column in the expanded screen
     */
    val widgetColumnWidth: Int

    /**
     *  The height of a widget's row in the expanded screen
     */
    val widgetRowHeight: Int

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

    @Parcelize
    data class CustomExpandedAppWidgetConfig(
        val spanX: Int,
        val spanY: Int,
        val index: Int,
        val showWhenLocked: Boolean
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
        val showWhenLocked: Boolean
    ): Parcelable

}

class ExpandedRepositoryImpl(
    context: Context,
    settings: SmartspacerSettingsRepository,
    private val databaseRepository: DatabaseRepository,
    private val widgetRepository: WidgetRepository,
    scope: CoroutineScope = MainScope()
): ExpandedRepository {

    override val overlayBackPressedBus = MutableSharedFlow<Unit>()
    override val overlayDragProgressChanged = MutableSharedFlow<Unit>()
    override val expandedCustomAppWidgets = databaseRepository.getExpandedCustomAppWidgets()

    private val displayPortraitWidth = context.getDisplayPortraitWidth()
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val maxWidgetWidth = displayPortraitWidth -
            (context.resources.getDimensionPixelSize(R.dimen.margin_16) * 2)
    private val maxWidgetHeight = context.resources
        .getDimensionPixelSize(R.dimen.expanded_smartspace_remoteviews_max_height)
    private val widgetContext = WidgetContextWrapper(context)
    private var appWidgetHostViews = HashMap<CacheTag, ExpandedAppWidgetHostView>()

    private val widgetsUseGoogleSans = settings.expandedWidgetUseGoogleSans.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, settings.expandedWidgetUseGoogleSans.getSync())

    @VisibleForTesting
    var appWidgetHost = ExpandedAppWidgetHost.create(widgetContext, 1).also {
        it.startListening()
    }

    override val widgetColumnWidth: Int = (maxWidgetWidth / 5f).toInt()
    override val widgetRowHeight: Int = context.resources.getDimensionPixelSize(
        R.dimen.expanded_smartspace_widget_row_height
    )

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
                showWhenLocked = customConfig.showWhenLocked
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
                it.showWhenLocked
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
        widget: Item.Widget,
        sessionId: String,
        handler: SmartspacerBasePageView.SmartspaceTargetInteractionListener
    ): ExpandedAppWidgetHostView {
        val appWidgetId = widget.appWidgetId
            ?: throw RuntimeException("Cannot create a widget without an ID!")
        val width = if(widget.width == 0){
            widget.provider.getWidth()
        }else{
            widget.width
        }.coerceAtMost(maxWidgetWidth)
        val height = if(widget.height == 0){
            widget.provider.getHeight()
        }else{
            widget.height
        }.let {
            //Only clip height of non-custom widgets
            if(widget.isCustom) it else it.coerceAtMost(maxWidgetHeight)
        }
        val cacheTag = CacheTag(appWidgetId, width, height, sessionId)
        appWidgetHostViews.destroyStaleHosts(cacheTag)
        appWidgetHostViews[cacheTag]?.let {
            return it
        }
        return appWidgetHost.createView(
            widgetContext, appWidgetId, sessionId, widget.provider, handler
        ).apply {
            this as ExpandedAppWidgetHostView
            updateSizeIfNeeded(width, height)
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
        }
        toDestroy.forEach {
            appWidgetHost.destroyView(it.value)
            remove(it.key)
        }
    }

    data class CacheTag(
        val appWidgetId: Int, val width: Int, val height: Int, val sessionId: String?
    )

}