package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.DeadSystemException
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.widget.RemoteViewsCompat.setImageViewImageAlpha
import androidx.core.widget.RemoteViewsCompat.setViewEnabled
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.ListWidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.ListWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.receivers.WidgetPageChangeReceiver
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.service.SmartspacerListWidgetRemoteViewsService
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuActivity
import com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility.AccessibilityPermissionActivity
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.Bitmap_createEmptyBitmap
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.dip
import com.kieronquinn.app.smartspacer.utils.extensions.dp
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.launch
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.screenOff
import com.kieronquinn.app.smartspacer.utils.extensions.setImageViewImageTintListCompat
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import com.kieronquinn.app.smartspacer.utils.remoteviews.FlagDisabledRemoteViews
import com.kieronquinn.app.smartspacer.widgets.SmartspacerAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

interface AppWidgetRepository {

    val newAppWidgetIdBus: Flow<Int>

    fun getPagedSessionState(appWidgetId: Int): PagedWidgetSmartspacerSessionState?
    fun getListSessionState(appWidgetId: Int): ListWidgetSmartspacerSessionState?
    fun Context.getPageRemoteViews(
        appWidgetId: Int,
        view: SmartspaceView,
        config: AppWidget?,
        isList: Boolean,
        overflowIntent: Intent?,
        container: (() -> RemoteViews) -> RemoteViews = { it() }
    ): RemoteViews
    fun Context.getPagedWidget(
        appWidgetId: Int,
        session: PagedWidgetSmartspacerSessionState? = getPagedSessionState(appWidgetId),
        config: AppWidget?
    ): RemoteViews?
    fun Context.getListWidget(widget: AppWidget): RemoteViews
    fun addWidget(
        appWidgetId: Int,
        ownerPackage: String,
        uiSurface: UiSurface,
        tintColour: TintColour,
        multiPage: Boolean,
        showControls: Boolean,
        animate: Boolean = true
    )
    fun deleteAppWidget(appWidgetId: Int)
    fun migrateAppWidget(oldAppWidgetId: Int, newAppWidgetId: Int)
    fun getAppWidget(appWidgetId: Int): AppWidget?
    suspend fun hasAppWidget(packageName: String): Boolean
    fun updateWidget(appWidgetId: Int)

    /**
     *  Gets the (portrait, landscape) [Rect] for the given app widget [id]. This is estimated, but
     *  is close enough for basic length calculation.
     */
    fun getAppWidgetSize(id: Int): Pair<Rect, Rect>

    /**
     *  Calculates the best max length for a [title] and optional [subtitle] with a given [width],
     *  for use in a widget. The [width] should already take into account margins etc., but the
     *  font size and typeface are handled here to save memory and CPU cycles.
     */
    fun getBestMaxLength(
        width: Int,
        size: Float,
        shadowEnabled: Boolean,
        title: CharSequence,
        subtitle: CharSequence?
    ): Pair<Int, Int?>

    fun supportsPinAppWidget(): Boolean
    fun requestPinAppWidget(callbackAction: String)
    fun onAppWidgetUpdate(vararg ids: Int)
    fun nextPage(appWidgetId: Int)
    fun previousPage(appWidgetId: Int)

    /**
     *  Removes orphaned app widgets based on a call to [AppWidgetManager.getAppWidgetIds],
     *  which also clears the accessibility notification if shown.
     */
    fun trimWidgets()

}

class AppWidgetRepositoryImpl(
    private val context: Context,
    private val databaseRepository: DatabaseRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val accessibilityRepository: AccessibilityRepository,
    private val notificationRepository: NotificationRepository,
    private val scope: CoroutineScope = MainScope()
): AppWidgetRepository {

    private val databaseLock = Mutex()
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    /**
     *  To prevent sending a full app widget update for every change to the list widgets, we send
     *  the main layout once, add the app widget ID to this set, and then only send an adapter
     *  change for future updates. If the widget becomes a paged widget, the ID will be removed from
     *  this set, and the main layout will be sent again if it becomes a list widget once more.
     */
    private val setUpListWidgets = HashSet<Int>()

    private val appWidgets = databaseRepository.getAppWidgets()
        .stateIn(scope, SharingStarted.Eagerly, null)

    @VisibleForTesting
    val widgetSessions = ArrayList<WidgetSmartspacerSession>()

    @VisibleForTesting
    val isLockscreenShowing = context.lockscreenShowing()

    @VisibleForTesting
    val screenOff = context.screenOff()

    private val maxLengthTextView by lazy {
        LayoutInflater.from(context)
            .inflate(R.layout.smartspacer_view_template_subtitle_measure, null) as TextView
    }

    override val newAppWidgetIdBus = MutableSharedFlow<Int>()

    override fun getPagedSessionState(appWidgetId: Int): PagedWidgetSmartspacerSessionState? {
        val session = widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }
        return (session as? PagedWidgetSmartspacerSession)?.state
    }

    override fun getListSessionState(appWidgetId: Int): ListWidgetSmartspacerSessionState? {
        val session = widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }
        return (session as? ListWidgetSmartspacerSession)?.state
    }

    override suspend fun hasAppWidget(packageName: String): Boolean {
        return appWidgets.firstNotNull().any { it.ownerPackage == packageName }
    }

    override fun updateWidget(appWidgetId: Int) {
        context.setupWidget(appWidgetId)
    }

    override fun addWidget(
        appWidgetId: Int,
        ownerPackage: String,
        uiSurface: UiSurface,
        tintColour: TintColour,
        multiPage: Boolean,
        showControls: Boolean,
        animate: Boolean
    ) {
        val widget = AppWidget(
            appWidgetId,
            ownerPackage,
            surface = uiSurface,
            tintColour = tintColour,
            multiPage = multiPage,
            showControls = showControls,
            animate = animate
        )
        scope.launch {
            databaseRepository.addAppWidget(widget)
        }
    }

    override fun deleteAppWidget(appWidgetId: Int) {
        scope.launch(databaseLock) {
            val widget = appWidgets.firstNotNull()
                .firstOrNull { it.appWidgetId == appWidgetId } ?: return@launch
            databaseRepository.deleteAppWidget(widget)
        }
    }

    override fun getAppWidget(appWidgetId: Int): AppWidget? {
        return runBlocking {
            //Don't block the thread for too long, fail if the database isn't loaded in time
            withTimeoutOrNull(2500L) {
                appWidgets.firstNotNull().firstOrNull { it.appWidgetId == appWidgetId }
            }
        }
    }

    override fun migrateAppWidget(oldAppWidgetId: Int, newAppWidgetId: Int) {
        scope.launch(databaseLock) {
            val current = appWidgets.firstNotNull()
                .firstOrNull { it.appWidgetId == oldAppWidgetId } ?: return@launch
            databaseRepository.deleteAppWidget(current)
            val new = current.cloneWithId(newAppWidgetId)
            databaseRepository.addAppWidget(new)
        }
    }

    override fun supportsPinAppWidget(): Boolean {
        return appWidgetManager.isRequestPinAppWidgetSupported
    }

    override fun requestPinAppWidget(callbackAction: String) {
        val callbackIntent = Intent(callbackAction).apply {
            `package` = context.packageName
        }
        val callback = PendingIntent.getBroadcast(
            context,
            UUID.randomUUID().hashCode(),
            callbackIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        appWidgetManager.requestPinAppWidget(
            ComponentName(context, SmartspacerAppWidgetProvider::class.java),
            null,
            callback
        )
    }

    override fun onAppWidgetUpdate(vararg ids: Int) {
        scope.launch {
            ids.forEach { id ->
                //Emit if we have a new unknown app widget ID, likely coming from a pin request
                if(appWidgets.firstNotNull().none { it.appWidgetId == id }) {
                    newAppWidgetIdBus.emit(id)
                }
            }
        }
    }

    override fun nextPage(appWidgetId: Int) {
        val session = widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }
                as? PagedWidgetSmartspacerSession
        session?.nextPage()
    }

    override fun previousPage(appWidgetId: Int) {
        val session = widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }
                as? PagedWidgetSmartspacerSession
        session?.previousPage()
    }

    override fun trimWidgets() {
        scope.launch {
            val component = ComponentName(context, SmartspacerAppWidgetProvider::class.java)
            val addedAppWidgetIds = appWidgetManager.getAppWidgetIds(component)
            val orphanedAppWidgets = databaseRepository.getAppWidgets().first().filterNot {
                addedAppWidgetIds.contains(it.appWidgetId)
            }
            orphanedAppWidgets.forEach {
                databaseRepository.deleteAppWidget(it)
            }
        }
    }

    private fun setupWidgets() {
        combine(
            appWidgets.debounce(250L).filterNotNull(),
            wallpaperRepository.homescreenWallpaperDarkTextColour
        ){ widgets, _ ->
            clearWidgetSessions()
            if(widgets.isNotEmpty() && !SmartspacerAccessibiltyService.isRunning(context)){
                showAccessibilityNotification()
            }else{
                notificationRepository.cancelNotification(NotificationId.ENABLE_ACCESSIBILITY)
            }
            widgetSessions.addAll(widgets.map { widget ->
                if(widget.listMode) {
                    ListWidgetSmartspacerSession(context, widget, collectInto = ::onWidgetChanged)
                }else{
                    PagedWidgetSmartspacerSession(context, widget, collectInto = ::onWidgetChanged)
                }
            })
        }.launchIn(scope)
    }

    private fun clearWidgetSessions() {
        widgetSessions.forEach {
            it.onDestroy()
        }
        widgetSessions.clear()
    }

    private fun onWidgetChanged(widget: AppWidget) {
        context.setupWidget(widget.appWidgetId)
    }

    override fun getAppWidgetSize(id: Int): Pair<Rect, Rect> {
        val options = appWidgetManager.getAppWidgetOptions(id)
        val portraitRect = Rect(
            0,
            0,
            context.dip(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)),
            context.dip(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT))
        )
        val landscapeRect = Rect(
            0,
            0,
            context.dip(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)),
            context.dip(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT))
        )
        return Pair(portraitRect, landscapeRect)
    }

    @Synchronized
    override fun getBestMaxLength(
        width: Int,
        size: Float,
        shadowEnabled: Boolean,
        title: CharSequence,
        subtitle: CharSequence?
    ): Pair<Int, Int?> {
        //If there's no subtitle, always include as much of the title as possible
        if(subtitle.isNullOrBlank()){
            return Pair(title.length, null)
        }

        val titleWidth = title.estimateWidth(size, shadowEnabled)
        val subtitleWidth = subtitle.estimateWidth(size, shadowEnabled)
        //If both will fit naturally, we can just show their full content
        if(titleWidth + subtitleWidth <= width){
            return Pair(title.length, subtitle.length)
        }
        //If clipping the title width by the subtitle width will fit, use that to fill the space
        val clippedTitleLength = title.getLengthForWidth(
            width - subtitleWidth, size, shadowEnabled
        )
        val clippedTitleWidth = title.subSequence(0, clippedTitleLength)
            .estimateWidth(size, shadowEnabled)
        if(clippedTitleWidth + subtitleWidth <= width){
            return Pair(clippedTitleLength, subtitle.length)
        }
        //Otherwise compromise and clip half way between
        val halfWidth = (width / 2f).toInt()
        return Pair(
            title.getLengthForWidth(halfWidth, size, shadowEnabled),
            subtitle.getLengthForWidth(halfWidth, size, shadowEnabled)
        )
    }

    private fun setupPackageStates() = scope.launch {
        combine(
            accessibilityRepository.foregroundPackage,
            isLockscreenShowing,
            screenOff
        ) { foregroundPackage, isLocked, isScreenOff ->
            widgetSessions.forEach { session ->
                val event = when {
                    isLocked && session.surface == UiSurface.LOCKSCREEN -> {
                        SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN
                    }
                    !isLocked && !isScreenOff && foregroundPackage == session.packageName -> {
                        SmartspaceTargetEvent.EVENT_UI_SURFACE_SHOWN
                    }
                    else -> SmartspaceTargetEvent.EVENT_UI_SURFACE_HIDDEN
                }
                session.notifySmartspaceEvent(
                    SmartspaceTargetEvent(null, null, event)
                )
            }
        }.launchIn(scope)
    }

    @VisibleForTesting
    fun showAccessibilityNotification() {
        notificationRepository.showNotification(
            NotificationId.ENABLE_ACCESSIBILITY,
            NotificationChannel.ACCESSIBILITY
        ) {
            it.setSmallIcon(R.drawable.ic_warning)
            it.setContentTitle(context.getString(R.string.notification_accessibility_widget_title))
            it.setContentText(
                context.getString(R.string.notification_accessibility_widget_content)
            )
            it.setContentIntent(
                PendingIntent.getActivity(
                context,
                NotificationId.ENABLE_ACCESSIBILITY.ordinal,
                Intent(context, AccessibilityPermissionActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setOngoing(true)
            it.setAutoCancel(false)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    init {
        setupWidgets()
        setupPackageStates()
    }

    private fun CharSequence.estimateWidth(textSize: Float, shadowEnabled: Boolean): Int {
        maxLengthTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        maxLengthTextView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if(shadowEnabled) {
            maxLengthTextView.setShadowLayer(1f, 1f, 1f, Color.BLACK)
        }else{
            maxLengthTextView.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        }
        maxLengthTextView.text = this
        maxLengthTextView.measure(0, 0)
        return maxLengthTextView.measuredWidth
    }

    private fun CharSequence.getLengthForWidth(
        width: Int,
        textSize: Float,
        shadowEnabled: Boolean
    ): Int {
        for(i in length downTo 0) {
            if(takeEllipsised(i).estimateWidth(textSize, shadowEnabled) <= width) return i
        }
        return 0
    }

    private fun Context.setupWidget(appWidgetId: Int) {
        val config = getAppWidget(appWidgetId)
        val remoteViews = when {
            config != null && config.listMode -> {
                if(setUpListWidgets.contains(appWidgetId)) {
                    //This widget is already set up, we can just send an update
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
                    return
                }
                getListWidget(config).also {
                    setUpListWidgets.add(appWidgetId)
                }
            }
            else -> {
                setUpListWidgets.remove(appWidgetId)
                getPagedWidget(appWidgetId, config = config)
            }
        } ?: RemoteViews(packageName, R.layout.widget_smartspacer_loading)
        try {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }catch (e: DeadSystemException) {
            //OS is shutting down
        }
    }

    override fun Context.getListWidget(widget: AppWidget): RemoteViews {
        val remoteViews = listContainer(widget) {
            RemoteViews(packageName, R.layout.widget_smartspacer_list)
        }
        remoteViews.setRemoteAdapter(
            R.id.widget_list,
            SmartspacerListWidgetRemoteViewsService.createIntent(
                context,
                widget.appWidgetId,
                widget.ownerPackage
            )
        )
        return remoteViews
    }

    private fun listContainer(
        widget: AppWidget,
        container: () -> RemoteViews
    ): RemoteViews {
        val textColour = widget.getTextColour()
        val shadowContainer = if(widget.showShadow && textColour == Color.WHITE) {
            RemoteViews(context.packageName, R.layout.widget_shadow_enabled)
        }else{
            RemoteViews(context.packageName, R.layout.widget_shadow_disabled)
        }
        shadowContainer.removeAllViews(R.id.root)
        shadowContainer.addView(R.id.root, container())
        return shadowContainer
    }

    override fun Context.getPagedWidget(
        appWidgetId: Int,
        session: PagedWidgetSmartspacerSessionState?,
        config: AppWidget?
    ): RemoteViews? {
        val textColour = config.getTextColour()
        val iconColour = ColorStateList.valueOf(textColour)
        val shadowEnabled = (config?.showShadow ?: true) && textColour == Color.WHITE
        return if(session != null) {
            getPageRemoteViews(appWidgetId, session.page.view, config, false, null) {
                container(
                    appWidgetId,
                    iconColour,
                    shadowEnabled,
                    config?.padding?.dp ?: 0,
                    session,
                    config?.ownerPackage,
                    it
                )
            }
        }else null
    }

    override fun Context.getPageRemoteViews(
        appWidgetId: Int,
        view: SmartspaceView,
        config: AppWidget?,
        isList: Boolean,
        overflowIntent: Intent?,
        container: (() -> RemoteViews) -> RemoteViews
    ): RemoteViews {
        val textColour = config.getTextColour()
        val shadowEnabled = (config?.showShadow ?: true) && textColour == Color.WHITE
        val titleSize = resources.getDimension(R.dimen.smartspace_view_title_size)
        val subtitleSize = resources.getDimension(R.dimen.smartspace_view_subtitle_size)
        val featureSize = resources.getDimension(R.dimen.smartspace_view_feature_size)
        val sizes = getAppWidgetSize(appWidgetId)
        val padding = config?.padding?.dp ?: 0
        val portrait = container {
            view.inflate(
                this,
                textColour,
                shadowEnabled,
                sizes.first.width(),
                titleSize,
                subtitleSize,
                featureSize,
                isList,
                overflowIntent,
                padding
            ).also {
                if(config?.hideControls == true) {
                    it.setImageViewBitmap(
                        R.id.widget_smartspacer_list_item_overflow,
                        Bitmap_createEmptyBitmap()
                    )
                }
                if(isList) {
                    it.setViewPadding(R.id.smartspace_view_root, padding, 0, padding, 0)
                }
            }
        }
        val landscape = container {
            view.inflate(
                this,
                textColour,
                shadowEnabled,
                sizes.second.width(),
                titleSize,
                subtitleSize,
                featureSize,
                isList,
                overflowIntent,
                padding
            ).also {
                if(config?.hideControls == true) {
                    it.setImageViewBitmap(
                        R.id.widget_smartspacer_list_item_overflow,
                        Bitmap_createEmptyBitmap()
                    )
                }
                if(isList) {
                    it.setViewPadding(R.id.smartspace_view_root, padding, 0, padding, 0)
                }
            }
        }
        return FlagDisabledRemoteViews(landscape, portrait)
    }

    private fun AppWidget?.getTextColour(): Int {
        return when(this?.tintColour ?: TintColour.AUTOMATIC) {
            TintColour.AUTOMATIC -> getWallpaperTextColour()
            TintColour.WHITE -> Color.WHITE
            TintColour.BLACK -> Color.BLACK
        }
    }

    private fun Context.container(
        appWidgetId: Int,
        iconColour: ColorStateList,
        shadowEnabled: Boolean,
        padding: Int,
        state: PagedWidgetSmartspacerSessionState,
        owner: String?,
        child: () -> RemoteViews
    ): RemoteViews {
        val container = RemoteViews(packageName, R.layout.widget_smartspacer)
        container.removeAllViews(R.id.widget_smartspacer_container_animated)
        container.removeAllViews(R.id.widget_smartspacer_container_no_animation)
        container.removeAllViews(R.id.widget_smartspacer_dots)
        container.setViewPadding(android.R.id.background, padding, 0, padding, 0)
        if(state.animate) {
            container.setViewVisibility(R.id.widget_smartspacer_container_animated, View.VISIBLE)
            container.setViewVisibility(R.id.widget_smartspacer_container_no_animation, View.GONE)
            container.addView(R.id.widget_smartspacer_container_animated, child())
        }else{
            container.setViewVisibility(R.id.widget_smartspacer_container_no_animation, View.VISIBLE)
            container.setViewVisibility(R.id.widget_smartspacer_container_animated, View.GONE)
            container.addView(R.id.widget_smartspacer_container_no_animation, child())
        }
        container.setOnClickPendingIntent(
            R.id.widget_smartspacer_previous,
            getPendingIntentForDirection(appWidgetId, WidgetPageChangeReceiver.Direction.PREVIOUS)
        )
        container.setImageViewImageTintListCompat(R.id.widget_smartspacer_previous, iconColour)
        container.setViewEnabled(R.id.widget_smartspacer_previous, !state.isFirst)
        container.setViewEnabled(R.id.widget_smartspacer_next, !state.isLast)
        container.setImageViewImageAlpha(R.id.widget_smartspacer_previous, if(state.isFirst) 0.5f else 1f)
        container.setImageViewImageAlpha(R.id.widget_smartspacer_next, if(state.isLast) 0.5f else 1f)
        container.setViewVisibility(R.id.widget_smartspacer_next, (!state.isOnlyPage && state.showControls).visibility)
        container.setViewVisibility(R.id.widget_smartspacer_previous, (!state.isOnlyPage && state.showControls).visibility)
        container.setViewVisibility(R.id.widget_smartspacer_dots, (!state.isOnlyPage).visibility)
        if(state.invisibleControls) {
            container.setImageViewBitmap(R.id.widget_smartspacer_next, Bitmap_createEmptyBitmap())
            container.setImageViewBitmap(R.id.widget_smartspacer_previous, Bitmap_createEmptyBitmap())
            container.setImageViewBitmap(R.id.widget_smartspacer_kebab, Bitmap_createEmptyBitmap())
        }
        container.setOnClickPendingIntent(
            R.id.widget_smartspacer_next,
            getPendingIntentForDirection(appWidgetId, WidgetPageChangeReceiver.Direction.NEXT)
        )
        container.setImageViewImageTintListCompat(R.id.widget_smartspacer_next, iconColour)
        val kebabPendingIntent = WidgetOptionsMenuActivity.getIntent(
            this, state.page.holder.page, appWidgetId, owner
        ).let {
            PendingIntent.getActivity(
                this,
                state.page.holder.page.hashCode(),
                it,
                PendingIntent_MUTABLE_FLAGS
            )
        }
        container.setImageViewImageTintListCompat(R.id.widget_smartspacer_kebab, iconColour)
        container.setOnClickPendingIntent(R.id.widget_smartspacer_kebab, kebabPendingIntent)
        state.dotConfig.getDots(packageName, iconColour).forEach {
            container.addView(R.id.widget_smartspacer_dots, it)
        }
        container.setOnClickPendingIntent(
            R.id.widget_smartspacer_dots, getPendingIntentForDirection(appWidgetId, WidgetPageChangeReceiver.Direction.NEXT)
        )
        val shadowContainer = if(shadowEnabled) {
            RemoteViews(packageName, R.layout.widget_shadow_enabled)
        }else{
            RemoteViews(packageName, R.layout.widget_shadow_disabled)
        }
        shadowContainer.removeAllViews(R.id.root)
        shadowContainer.addView(R.id.root, container)
        return shadowContainer
    }

    private fun Context.getPendingIntentForDirection(
        appWidgetId: Int,
        direction: WidgetPageChangeReceiver.Direction
    ): PendingIntent {
        val id = "${NotificationId.WIDGET_DIRECTION.ordinal}$appWidgetId${direction.ordinal}".toInt()
        return PendingIntent.getBroadcast(
            this,
            id,
            WidgetPageChangeReceiver.getIntent(this, appWidgetId, direction),
            PendingIntent_MUTABLE_FLAGS
        )
    }

    private fun List<PagedWidgetSmartspacerSessionState.DotConfig>.getDots(
        packageName: String,
        dotColour: ColorStateList
    ): List<RemoteViews> {
        return map {
            when(it) {
                PagedWidgetSmartspacerSessionState.DotConfig.REGULAR -> {
                    RemoteViews(packageName, R.layout.widget_dot).apply {
                        setImageViewImageTintListCompat(R.id.dot, dotColour)
                        setImageViewImageAlpha(R.id.dot, 0.5f)
                    }
                }
                PagedWidgetSmartspacerSessionState.DotConfig.HIGHLIGHTED -> {
                    RemoteViews(packageName, R.layout.widget_dot).apply {
                        setImageViewImageTintListCompat(R.id.dot, dotColour)
                        setImageViewImageAlpha(R.id.dot, 1f)
                    }
                }
            }
        }
    }

    private fun getWallpaperTextColour(): Int {
        return if(wallpaperRepository.homescreenWallpaperDarkTextColour.value){
            Color.BLACK
        }else{
            Color.WHITE
        }
    }

    private fun RemoteViews.setImageViewImageAlpha(id: Int, alpha: Float) {
        setImageViewImageAlpha(id, (alpha * 255).toInt())
    }

    private val Boolean.visibility
        get() = if(this) View.VISIBLE else View.GONE

}