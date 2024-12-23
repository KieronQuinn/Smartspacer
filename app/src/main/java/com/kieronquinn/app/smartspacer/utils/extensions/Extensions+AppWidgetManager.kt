package com.kieronquinn.app.smartspacer.utils.extensions

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManagerHidden
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.UserManager
import android.util.Log
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
    return try {
        Refine.unsafeCast<AppWidgetManagerHidden>(this)
            .bindRemoteViewsService(context, appWidgetId, intent, dispatcher, flags)
    }catch (e: SecurityException) {
        //App installed on another user, seems to be Xiaomi issue
        return false
    }
}

fun AppWidgetManager.noteAppWidgetTappedCompat(appWidgetId: Int) {
    this as AppWidgetManagerHidden
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        noteAppWidgetTapped(appWidgetId)
    }
}

fun AppWidgetManager.getAllInstalledProviders(context: Context): List<AppWidgetProviderInfo> {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    return userManager.userProfiles.flatMap {
        getInstalledProvidersForProfile(it)
    }
}