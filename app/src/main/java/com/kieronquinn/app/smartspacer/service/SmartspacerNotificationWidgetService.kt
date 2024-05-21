package com.kieronquinn.app.smartspacer.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.core.widget.RemoteViewsCompat.setImageViewImageAlpha
import androidx.core.widget.RemoteViewsCompat.setImageViewImageTintList
import androidx.core.widget.RemoteViewsCompat.setViewEnabled
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.Smartspacer
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.NotificationSmartspacerSession
import com.kieronquinn.app.smartspacer.components.smartspace.PagedWidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.model.database.AppWidget
import com.kieronquinn.app.smartspacer.receivers.WidgetPageChangeReceiver
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuActivity
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.getDarkMode
import com.kieronquinn.app.smartspacer.utils.extensions.isLockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.isServiceRunning
import com.kieronquinn.app.smartspacer.utils.extensions.lockscreenShowing
import com.kieronquinn.app.smartspacer.utils.extensions.startForeground
import com.kieronquinn.app.smartspacer.utils.extensions.whenCreated
import com.kieronquinn.app.smartspacer.widgets.SmartspacerAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.ext.android.inject

class SmartspacerNotificationWidgetService: LifecycleService() {

    companion object {
        private const val FAKE_APP_WIDGET_ID = Integer.MAX_VALUE / 1000

        fun startServiceIfNeeded(context: Context) {
            if(isServiceRunning(context)) return
            val intent = Intent(context, SmartspacerNotificationWidgetService::class.java)
            try {
                context.startService(intent)
            }catch (e: Exception) {
                //Not allowed to start
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SmartspacerNotificationWidgetService::class.java)
            context.stopService(intent)
        }

        private fun isServiceRunning(context: Context): Boolean {
            return context.isServiceRunning(SmartspacerNotificationWidgetService::class.java)
        }
    }

    private val notifications by inject<NotificationRepository>()
    private val settings by inject<SmartspacerSettingsRepository>()

    private val changeBus = MutableStateFlow(System.currentTimeMillis())

    private val lockscreenShowing by lazy {
        lockscreenShowing()
            .stateIn(lifecycleScope, SharingStarted.Eagerly, isLockscreenShowing())
    }

    private val tintColour by lazy {
        combine(
            getDarkMode(lifecycleScope),
            settings.notificationWidgetTintColour.asFlow()
        ) { isDarkMode, tintColour ->
            when(tintColour) {
                TintColour.AUTOMATIC -> if(isDarkMode) {
                    Color.WHITE
                } else {
                    Color.BLACK
                }
                TintColour.BLACK -> Color.BLACK
                TintColour.WHITE -> Color.WHITE
            }
        }
    }

    private val widget by lazy {
        AppWidget(
            FAKE_APP_WIDGET_ID, //Not actually used
            Smartspacer.PACKAGE_KEYGUARD,
            UiSurface.LOCKSCREEN,
            TintColour.AUTOMATIC, //Not actually used
            multiPage = false, //Not actually used
            showControls = false //Not actually used
        )
    }

    private val session by lazy {
        NotificationSmartspacerSession(
            this,
            widget,
            collectInto = ::onWidgetChanged
        )
    }

    private val titleSize by lazy {
        resources.getDimension(R.dimen.smartspace_view_notification_title_size)
    }

    private val subtitleSize by lazy {
        resources.getDimension(R.dimen.smartspace_view_notification_subtitle_size)
    }

    private val featureSize by lazy {
        resources.getDimension(R.dimen.smartspace_view_notification_feature_size)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationId.NOTIFICATION_SERVICE, createNotification())
        setupResumeState()
        setupState()
    }

    override fun onDestroy() {
        super.onDestroy()
        session.onDestroy()
    }

    private fun setupResumeState() = whenCreated {
        lockscreenShowing.collect { showing ->
            if(showing) {
                session.onResume()
            }else{
                session.onPause()
            }
        }
    }

    private fun setupState() = whenCreated {
        combine(
            lockscreenShowing,
            tintColour,
            changeBus
        ) { shouldShow, tint, _ ->
            val state = session.state
            val view = state?.page?.view
            val basicView = state?.page?.basicView
            val notification = if(shouldShow && view != null && basicView != null) {
                createNotification(state, tint, view, basicView)
            }else null
            if(notification != null) {
                notifications.showNotification(
                    NotificationId.SMARTSPACER_WIDGET_NOTIFICATION,
                    NotificationChannel.WIDGET_NOTIFICATION
                ) {
                    notification(it)
                }
            }else{
                notifications.cancelNotification(NotificationId.SMARTSPACER_WIDGET_NOTIFICATION)
            }
        }.collect()
    }

    private suspend fun onWidgetChanged(widget: AppWidget) {
        changeBus.emit(System.currentTimeMillis())
    }

    private fun createNotification(
        state: PagedWidgetSmartspacerSessionState,
        tint: Int,
        view: SmartspaceView,
        basicView: SmartspaceView
    ) = { it: NotificationCompat.Builder ->
        val remoteViews = basicView.inflate(
            this@SmartspacerNotificationWidgetService,
            tint,
            false,
            getUsableWidth(),
            titleSize,
            subtitleSize,
            featureSize,
            false,
            null,
            0
        )
        val container = SmartspacerAppWidgetProvider().run {
            container(
                R.layout.remoteviews_container_notification_small,
                FAKE_APP_WIDGET_ID,
                ColorStateList.valueOf(Color.WHITE),
                state,
                state.config.packageName,
                showControls = false,
                showDots = false
            ) {
                remoteViews
            }
        }
        val expandedRemoteViews = view.inflate(
            this@SmartspacerNotificationWidgetService,
            tint,
            false,
            getUsableWidth(),
            titleSize,
            subtitleSize,
            featureSize,
            false,
            null,
            0
        )
        val expandedContainer = SmartspacerAppWidgetProvider().run {
            container(
                R.layout.remoteviews_container_notification_large,
                FAKE_APP_WIDGET_ID,
                ColorStateList.valueOf(Color.WHITE),
                state,
                state.config.packageName,
                showControls = false,
                showDots = false
            ) {
                expandedRemoteViews
            }
        }
        it.priority = Notification.PRIORITY_MAX
        it.setGroup("widget")
        it.setOngoing(true)
        it.setWhen(System.currentTimeMillis())
        it.setOnlyAlertOnce(true)
        it.setSilent(true)
        it.setCustomContentView(container)
        it.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        it.setCustomBigContentView(expandedContainer)
        it.setSmallIcon(R.drawable.ic_notification)
        it.setContentTitle(getString(R.string.app_name))
        it.setContentText(getString(R.string.notification_widget_title))
    }

    private fun Context.container(
        layoutRes: Int,
        appWidgetId: Int,
        iconColour: ColorStateList,
        state: PagedWidgetSmartspacerSessionState,
        owner: String?,
        showControls: Boolean = state.showControls,
        showDots: Boolean = !state.isOnlyPage,
        child: () -> RemoteViews
    ): RemoteViews {
        val container = RemoteViews(packageName, layoutRes)
        val padding = resources.getDimensionPixelSize(R.dimen.margin_16)
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            container.setViewPadding(android.R.id.background, padding, padding, padding, padding)
        }
        container.removeAllViews(R.id.widget_smartspacer_container_animated)
        container.removeAllViews(R.id.widget_smartspacer_container_no_animation)
        container.removeAllViews(R.id.widget_smartspacer_dots)
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
        container.setViewVisibility(R.id.widget_smartspacer_next, (!state.isOnlyPage).visibility)
        container.setViewVisibility(R.id.widget_smartspacer_previous, (!state.isOnlyPage).visibility)
        //No point showing arrows or dots if there's only one page
        container.setViewVisibility(R.id.widget_smartspacer_dots, showDots.visibility)
        container.setViewVisibility(R.id.widget_smartspacer_controls, showControls.visibility)
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
        val shadowContainer = RemoteViews(packageName, R.layout.widget_shadow_disabled)
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

    private fun RemoteViews.setImageViewImageAlpha(id: Int, alpha: Float) {
        setImageViewImageAlpha(id, (alpha * 255).toInt())
    }

    private fun RemoteViews.setImageViewImageTintListCompat(
        id: Int,
        colourStateList: ColorStateList
    ) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setImageViewImageTintList(id, colourStateList)
        }else{
            setImageViewColorFilter(id, colourStateList.defaultColor)
        }
    }

    private val Boolean.visibility
        get() = if(this) View.VISIBLE else View.GONE

    private fun createNotification(): Notification {
        return notifications.showNotification(
            NotificationId.NOTIFICATION_SERVICE,
            NotificationChannel.BACKGROUND_SERVICE
        ) {
            val notificationIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannel.BACKGROUND_SERVICE.id)
            }
            it.setContentTitle(getString(R.string.notification_title_background_service))
            it.setContentText(getString(R.string.notification_content_background_service))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.MANAGER_SERVICE.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_title_background_service))
        }
    }

    private fun getUsableWidth(): Int {
        val displayWidth = Resources.getSystem().displayMetrics.widthPixels
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            displayWidth - this.edgeDistancePreS()
        } else {
            displayWidth - this.edgeDistanceS()
        }
    }

    private fun edgeDistanceS(): Int {
        return resources
            .getDimensionPixelSize(R.dimen.smartspace_view_notification_screen_edges_distance_s)
    }

    private fun edgeDistancePreS(): Int {
        return resources
            .getDimensionPixelSize(R.dimen.smartspace_view_notification_edges_distance_before_s)
    }

}