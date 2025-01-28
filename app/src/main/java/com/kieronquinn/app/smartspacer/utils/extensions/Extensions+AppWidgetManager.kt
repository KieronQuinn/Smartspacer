package com.kieronquinn.app.smartspacer.utils.extensions

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManagerHidden
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.UserManager
import android.util.SizeF
import dev.rikka.tools.refine.Refine
import kotlin.math.max
import kotlin.math.min

fun AppWidgetManager.bindRemoteViewsService(
    context: Context,
    appWidgetId: Int,
    intent: Intent,
    serviceConnection: ServiceConnection,
    handler: Handler = context.getMainThreadHandler(),
    flags: Int = Context.BIND_AUTO_CREATE or 0x02000000 //BIND_FOREGROUND_SERVICE_WHILE_AWAKE
): Boolean {
    if(isAtLeastBaklava()) {
        //Not supported anymore, collection item migration mechanic will be used instead.
        return false
    }else{
        val dispatcher = context.getServiceDispatcher(serviceConnection, handler, flags)
        return try {
            Refine.unsafeCast<AppWidgetManagerHidden>(this)
                .bindRemoteViewsService(context, appWidgetId, intent, dispatcher, flags)
        }catch (e: SecurityException) {
            //App installed on another user, seems to be Xiaomi issue
            return false
        }
    }
}

/**
 *  Same as [AppWidgetHostView.updateAppWidgetSize], but without the need for a host view. This
 *  allows initialising the widget immediately after binding, to fix widgets which take the size
 *  immediately (like KWGT).
 */
fun AppWidgetManager.initialiseAppWidgetSize(
    context: Context,
    appWidgetId: Int,
    width: Float,
    height: Float
) {
    val padding: Rect = AppWidgetHostView.getDefaultPaddingForWidget(
        context,
        ComponentName("package", "class"), //This is never used
        null
    )
    val density: Float = context.resources.displayMetrics.density
    val sizes = listOf(SizeF(width, height))
    val newOptions = getAppWidgetOptions(appWidgetId)

    val xPaddingDips = (padding.left + padding.right) / density
    val yPaddingDips = (padding.top + padding.bottom) / density

    val paddedSizes = ArrayList<SizeF>(sizes.size)
    var minWidth = Float.MAX_VALUE
    var maxWidth = 0f
    var minHeight = Float.MAX_VALUE
    var maxHeight = 0f
    for (i in sizes.indices) {
        val size = sizes[i]
        val paddedSize = SizeF(
            max(0.0, (size.width - xPaddingDips).toDouble()).toFloat(),
            max(0.0, (size.height - yPaddingDips).toDouble()).toFloat()
        )
        paddedSizes.add(paddedSize)
        minWidth = min(minWidth.toDouble(), paddedSize.width.toDouble()).toFloat()
        maxWidth = max(maxWidth.toDouble(), paddedSize.width.toDouble()).toFloat()
        minHeight = min(minHeight.toDouble(), paddedSize.height.toDouble()).toFloat()
        maxHeight = max(maxHeight.toDouble(), paddedSize.height.toDouble()).toFloat()
    }
    if (paddedSizes == getAppWidgetOptions(appWidgetId)
            .getParcelableArrayList<SizeF>(
                AppWidgetManager.OPTION_APPWIDGET_SIZES
            )
    ) {
        return
    }
    val options = newOptions.deepCopy()
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth.toInt())
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight.toInt())
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth.toInt())
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight.toInt())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, paddedSizes)
    }
    updateAppWidgetOptions(appWidgetId, options)
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