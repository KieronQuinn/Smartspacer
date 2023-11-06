package com.kieronquinn.app.smartspacer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.DeadSystemException
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.core.widget.RemoteViewsCompat.setImageViewImageAlpha
import androidx.core.widget.RemoteViewsCompat.setImageViewImageTintList
import androidx.core.widget.RemoteViewsCompat.setViewEnabled
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId.WIDGET_DIRECTION
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSessionState
import com.kieronquinn.app.smartspacer.components.smartspace.WidgetSmartspacerSessionState.DotConfig
import com.kieronquinn.app.smartspacer.receivers.WidgetPageChangeReceiver
import com.kieronquinn.app.smartspacer.receivers.WidgetPageChangeReceiver.Direction
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.ui.activities.WidgetOptionsMenuActivity
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.stripData
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspacerAppWidgetProvider: AppWidgetProvider(), KoinComponent {

    private val appWidgetRepository by inject<AppWidgetRepository>()
    private val wallpaperRepository by inject<WallpaperRepository>()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            context.setupWidget(id)
        }
        appWidgetRepository.onAppWidgetUpdate(*appWidgetIds)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        oldWidgetIds.zip(newWidgetIds).forEach {
            appWidgetRepository.migrateAppWidget(it.first, it.second)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            appWidgetRepository.deleteAppWidget(id)
        }
    }

    private fun Context.setupWidget(appWidgetId: Int) {
        val appWidgetManager = getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
        val config = appWidgetRepository.getAppWidget(appWidgetId)
        val state = appWidgetRepository.getSessionState(appWidgetId)
        val textColour = when(config?.tintColour ?: TintColour.AUTOMATIC) {
            TintColour.AUTOMATIC -> getWallpaperTextColour()
            TintColour.WHITE -> Color.WHITE
            TintColour.BLACK -> Color.BLACK
        }
        val iconColour = ColorStateList.valueOf(textColour)
        val remoteViews = state?.page?.view?.let {
            val sizes = appWidgetRepository.getAppWidgetSize(appWidgetId)
            val portrait = container(appWidgetId, iconColour, state, config?.ownerPackage) {
                it.inflate(this, textColour, sizes.first.width())
            }
            val landscape = container(appWidgetId, iconColour, state, config?.ownerPackage) {
                it.inflate(this, textColour, sizes.second.width())
            }
            RemoteViews(landscape, portrait)
        } ?: RemoteViews(packageName, R.layout.widget_smartspacer_loading)
        try {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }catch (e: DeadSystemException) {
            //OS is shutting down
        }
    }

    private fun Context.container(
        appWidgetId: Int,
        iconColour: ColorStateList,
        state: WidgetSmartspacerSessionState,
        owner: String?,
        child: () -> RemoteViews
    ): RemoteViews {
        val container = RemoteViews(packageName, R.layout.widget_smartspacer)
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
            getPendingIntentForDirection(appWidgetId, Direction.PREVIOUS)
        )
        container.setImageViewImageTintListCompat(R.id.widget_smartspacer_previous, iconColour)
        container.setViewEnabled(R.id.widget_smartspacer_previous, !state.isFirst)
        container.setViewEnabled(R.id.widget_smartspacer_next, !state.isLast)
        container.setImageViewImageAlpha(R.id.widget_smartspacer_previous, if(state.isFirst) 0.5f else 1f)
        container.setImageViewImageAlpha(R.id.widget_smartspacer_next, if(state.isLast) 0.5f else 1f)
        container.setViewVisibility(R.id.widget_smartspacer_next, (!state.isOnlyPage).visibility)
        container.setViewVisibility(R.id.widget_smartspacer_previous, (!state.isOnlyPage).visibility)
        //No point showing arrows or dots if there's only one page
        container.setViewVisibility(R.id.widget_smartspacer_dots, (!state.isOnlyPage).visibility)
        container.setViewVisibility(R.id.widget_smartspacer_controls, state.showControls.visibility)
        container.setOnClickPendingIntent(
            R.id.widget_smartspacer_next,
            getPendingIntentForDirection(appWidgetId, Direction.NEXT)
        )
        container.setImageViewImageTintListCompat(R.id.widget_smartspacer_next, iconColour)
        val kebabPendingIntent = WidgetOptionsMenuActivity.getIntent(
            this, state.page.holder.page.stripData(), appWidgetId, owner
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
            R.id.widget_smartspacer_dots, getPendingIntentForDirection(appWidgetId, Direction.NEXT)
        )
        return container
    }

    private fun Context.getPendingIntentForDirection(
        appWidgetId: Int,
        direction: Direction
    ): PendingIntent {
        val id = "${WIDGET_DIRECTION.ordinal}$appWidgetId${direction.ordinal}".toInt()
        return PendingIntent.getBroadcast(
            this,
            id,
            WidgetPageChangeReceiver.getIntent(this, appWidgetId, direction),
            PendingIntent_MUTABLE_FLAGS
        )
    }

    private fun List<DotConfig>.getDots(
        packageName: String,
        dotColour: ColorStateList
    ): List<RemoteViews> {
        return map {
            when(it) {
                DotConfig.REGULAR -> {
                    RemoteViews(packageName, R.layout.widget_dot).apply {
                        setImageViewImageTintListCompat(R.id.dot, dotColour)
                        setImageViewImageAlpha(R.id.dot, 0.5f)
                    }
                }
                DotConfig.HIGHLIGHTED -> {
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

}