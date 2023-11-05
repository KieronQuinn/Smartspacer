package com.kieronquinn.app.smartspacer.repositories

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.app.NotificationCompat
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTargetEvent
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.ui.activities.permission.accessibility.AccessibilityPermissionActivity
import com.kieronquinn.app.smartspacer.utils.extensions.dip
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.launch
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.screenOff
import com.kieronquinn.app.smartspacer.widgets.SmartspacerAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
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

    fun getSessionState(appWidgetId: Int): WidgetSmartspacerSessionState?
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
    fun getBestMaxLength(width: Int, title: CharSequence, subtitle: CharSequence?): Pair<Int, Int?>

    fun supportsPinAppWidget(): Boolean
    fun requestPinAppWidget(callbackAction: String)
    fun onAppWidgetUpdate(vararg ids: Int)
    fun nextPage(appWidgetId: Int)
    fun previousPage(appWidgetId: Int)

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

    private val appWidgets = databaseRepository.getAppWidgets()
        .stateIn(scope, SharingStarted.Eagerly, null)

    @VisibleForTesting
    val widgetSessions = ArrayList<WidgetSmartspacerSession>()

    @VisibleForTesting
    val isLockscreenShowing = context.lockscreenShowing()

    @VisibleForTesting
    val screenOff = context.screenOff()

    private val maxLengthTextPaint by lazy {
        TextPaint().apply {
            textSize = context.resources.getDimensionPixelSize(R.dimen.smartspace_view_title_size).toFloat()
            typeface = Typeface.create("google-sans", Typeface.NORMAL)
        }
    }

    override val newAppWidgetIdBus = MutableSharedFlow<Int>()

    override fun getSessionState(appWidgetId: Int): WidgetSmartspacerSessionState? {
        return widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }?.state
    }

    override suspend fun hasAppWidget(packageName: String): Boolean {
        return appWidgets.firstNotNull().any { it.ownerPackage == packageName }
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
                if(appWidgets.firstNotNull().none { it.appWidgetId == id}) {
                    newAppWidgetIdBus.emit(id)
                }
            }
        }
    }

    override fun nextPage(appWidgetId: Int) {
        widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }?.nextPage()
    }

    override fun previousPage(appWidgetId: Int) {
        widgetSessions.firstOrNull { it.appWidgetId == appWidgetId }?.previousPage()
    }

    private fun setupWidgets() {
        combine(
            appWidgets.debounce(250L).filterNotNull(),
            wallpaperRepository.homescreenWallpaperDarkTextColour
        ){ widgets, _ ->
            clearWidgetSessions()
            if(widgets.isNotEmpty() && !SmartspacerAccessibiltyService.isRunning(context)){
                showAccessibilityNotification()
            }
            widgetSessions.addAll(widgets.map { widget ->
                WidgetSmartspacerSession(context, widget, collectInto = ::onWidgetChanged)
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
        Intent(context, SmartspacerAppWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widget.appWidgetId))
        }.also {
            context.sendBroadcast(it)
        }
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
    override fun getBestMaxLength(width: Int, title: CharSequence, subtitle: CharSequence?): Pair<Int, Int?> {
        //If there's no subtitle, always include as much of the title as possible
        if(subtitle.isNullOrBlank()){
            return Pair(title.length, null)
        }
        val titleWidth = maxLengthTextPaint.calculateWidth(title)
        val subtitleWidth = maxLengthTextPaint.calculateWidth(subtitle)
        //If both will fit naturally, we can just show their full content
        if(titleWidth + subtitleWidth <= width){
            return Pair(title.length, subtitle.length)
        }
        //If clipping the title width by the subtitle width will fit, use that to fill the space
        val clippedTitleLength = maxLengthTextPaint
            .getLengthForWidth(title, width - subtitleWidth)
        val clippedTitleWidth = maxLengthTextPaint
            .calculateWidth(title.subSequence(0, clippedTitleLength))
        if(clippedTitleWidth + subtitleWidth <= width){
            return Pair(clippedTitleLength, subtitle.length)
        }
        //Otherwise compromise and clip half way between
        val halfWidth = (width / 2f).toInt()
        return Pair(
            maxLengthTextPaint.getLengthForWidth(title, halfWidth),
            maxLengthTextPaint.getLengthForWidth(subtitle, halfWidth)
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

    /**
     *  Calculates the width of a given [text] within the [TextPaint]
     */
    private fun TextPaint.calculateWidth(text: CharSequence): Int {
        val rect = Rect()
        getTextBounds(text, 0, text.length, rect)
        return rect.width()
    }

    /**
     *  Gets the best length for given [text] to fit into [width]; that is the highest length that
     *  will fit in the width
     */
    private fun TextPaint.getLengthForWidth(text: CharSequence, width: Int): Int {
        for(i in text.length downTo 0) {
            if(calculateWidth(text.subSequence(0, i)) <= width) return i
        }
        return 0
    }

}