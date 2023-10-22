package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostHidden
import android.appwidget.AppWidgetHostView
import android.content.IntentSender
import android.util.SparseArray
import com.android.internal.appwidget.IAppWidgetService
import com.kieronquinn.app.smartspacer.BuildConfig

@SuppressLint("DiscouragedPrivateApi")
fun AppWidgetHost.getIntentSenderForConfigureActivityCompat(
    appWidgetId: Int, intentFlags: Int
): IntentSender {
    val sService = AppWidgetHost::class.java.getDeclaredField("sService").apply {
        isAccessible = true
    }.get(this) as IAppWidgetService
    return sService.createAppWidgetConfigIntentSender(
        BuildConfig.APPLICATION_ID, appWidgetId, intentFlags
    )
}

@SuppressLint("SoonBlockedPrivateApi")
fun AppWidgetHost.removeListener(view: AppWidgetHostView) {
    val appWidgetId = view.appWidgetId
    if(removeListenerByMethod(appWidgetId)) return
    if(removeListenerByListeners(appWidgetId)) return
    if(removeListenerByViews(appWidgetId)) return
}

private fun AppWidgetHost.removeListenerByMethod(appWidgetId: Int): Boolean {
    return try {
        this as AppWidgetHostHidden
        removeListener(appWidgetId)
        true
    }catch (e: Throwable){
        false
    }
}

@SuppressLint("SoonBlockedPrivateApi")
private fun AppWidgetHost.removeListenerByListeners(appWidgetId: Int): Boolean {
    return try {
        val mListeners = AppWidgetHost::class.java.getDeclaredField("mListeners").apply {
            isAccessible = true
        }.get(this) as SparseArray<AppWidgetHostView>
        synchronized(mListeners){
            mListeners.remove(appWidgetId)
        }
        true
    }catch (e: Throwable){
        false
    }
}

@SuppressLint("SoonBlockedPrivateApi")
private fun AppWidgetHost.removeListenerByViews(appWidgetId: Int): Boolean {
    return try {
        val mViews = AppWidgetHost::class.java.getDeclaredField("mViews").apply {
            isAccessible = true
        }.get(this) as SparseArray<AppWidgetHostView>
        synchronized(mViews){
            mViews.remove(appWidgetId)
        }
        true
    }catch (e: Throwable){
        false
    }
}