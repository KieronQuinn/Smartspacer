package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.AppWidgetHostViewHidden
import android.widget.RemoteViewsHidden.InteractionHandler
import android.widget.RemoteViewsHidden.OnClickHandler
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView.SmartspaceTargetInteractionListener.Companion.launchAction
import dev.rikka.tools.refine.Refine

@Suppress("DEPRECATION")
fun AppWidgetHostView.updateAppWidgetSize(width: Int, height: Int, options: Bundle) {
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        updateAppWidgetSize(options, listOf(SizeF(width.toFloat(), height.toFloat())))
    }else{
        updateAppWidgetSize(options, width, height, width, height)
    }
}

@SuppressLint("SoonBlockedPrivateApi")
fun AppWidgetHostView.resetAppWidget(info: AppWidgetProviderInfo?) {
    AppWidgetHostView::class.java.getDeclaredMethod(
        "resetAppWidget", AppWidgetProviderInfo::class.java
    ).apply {
        isAccessible = true
    }.invoke(this, info)
}

@SuppressLint("SoonBlockedPrivateApi")
fun AppWidgetHostView.viewDataChanged(viewId: Int) {
    AppWidgetHostView::class.java.getDeclaredMethod("viewDataChanged", Integer.TYPE).apply {
        isAccessible = true
    }.invoke(this, viewId)
}

fun AppWidgetHostView.setInteractionHandler(
    interactionListener: SmartspaceTargetInteractionListener
) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val handler = InteractionHandler { view, pendingIntent, response ->
            interactionListener.launchAction(pendingIntent?.isActivity ?: true) {
                RemoteViews_startPendingIntent(view, pendingIntent, response.getLaunchOptions(view))
            }
            true
        }
        Refine.unsafeCast<AppWidgetHostViewHidden>(this).setInteractionHandler(handler)
    }else{
        val handler = OnClickHandler { view, pendingIntent, response ->
            interactionListener.launchAction(true) {
                RemoteViews_startPendingIntent(view, pendingIntent, response.getLaunchOptions(view))
            }
            true
        }
        Refine.unsafeCast<AppWidgetHostViewHidden>(this).setOnClickHandler(handler)
    }
}