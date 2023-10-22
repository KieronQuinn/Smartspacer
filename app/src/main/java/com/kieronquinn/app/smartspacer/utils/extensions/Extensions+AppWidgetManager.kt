package com.kieronquinn.app.smartspacer.utils.extensions

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManagerHidden
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import dev.rikka.tools.refine.Refine

fun AppWidgetManager.bindRemoteViewsService(
    context: Context,
    appWidgetId: Int,
    intent: Intent,
    serviceConnection: ServiceConnection,
    handler: Handler = context.getMainThreadHandler(),
    flags: Int = Context.BIND_AUTO_CREATE or 0x02000000 //BIND_FOREGROUND_SERVICE_WHILE_AWAKE
): Boolean {
    val dispatcher = context.getServiceDispatcher(serviceConnection, handler, flags)
    return Refine.unsafeCast<AppWidgetManagerHidden>(this)
        .bindRemoteViewsService(context, appWidgetId, intent, dispatcher, flags)
}