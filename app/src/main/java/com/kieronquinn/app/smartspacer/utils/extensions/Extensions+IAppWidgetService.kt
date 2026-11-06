package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.IntentSender
import android.os.Process
import com.android.internal.appwidget.IAppWidgetService

fun IAppWidgetService.createAppWidgetConfigIntentSenderCompat(
    callingPackage: String,
    appWidgetId: Int,
    intentFlags: Int
): IntentSender {
    val userId = Process.myUserHandle().getIdentifier()
    return try {
        createAppWidgetConfigIntentSender(callingPackage, appWidgetId, intentFlags, userId)
    } catch (e: NoSuchMethodError) {
        createAppWidgetConfigIntentSender(callingPackage, appWidgetId, intentFlags)
    }
}