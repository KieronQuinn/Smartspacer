package com.kieronquinn.app.smartspacer.utils.extensions

import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfoHidden
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.InflateException
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.ExpandedRepository
import dev.rikka.tools.refine.Refine
import org.koin.java.KoinJavaComponent.get

private val resources = Resources.getSystem()

var AppWidgetProviderInfo.providerInfo: ActivityInfo
    get() {
        return Refine.unsafeCast<AppWidgetProviderInfoHidden>(this).providerInfo
    }
    set(value) {
        Refine.unsafeCast<AppWidgetProviderInfoHidden>(this).providerInfo = value
    }

fun AppWidgetProviderInfo.loadPreview(context: Context, parent: ViewGroup): View? {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || previewLayout == 0) return null
    val packageContext = context.applicationContext.createPackageContextOrNull(
        provider.packageName, Context.CONTEXT_RESTRICTED
    ) ?: return null
    val widgetContext = ContextThemeWrapper(packageContext, R.style.Theme_Smartspacer)
    return try {
        RemoteViews(provider.packageName, previewLayout).apply(widgetContext, parent)
    }catch (e: InflateException){
        null
    }
}

fun AppWidgetProviderInfo.getWidthSpan(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetCellWidth != 0) {
        targetCellWidth
    }else{
        calculateColumnSpan(minWidth).coerceAtLeast(1)
    }
}

fun AppWidgetProviderInfo.getHeightSpan(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetCellHeight != 0) {
        targetCellHeight
    }else{
        calculateColumnSpan(minHeight).coerceAtLeast(1)
    }
}

fun AppWidgetProviderInfo.isFourByOne(): Boolean {
    return getWidthSpan() == 4 && getHeightSpan() == 1
}

fun AppWidgetProviderInfo.getWidth(): Int {
    return roundToColumnSpan(minWidth)
}

fun AppWidgetProviderInfo.getHeight(): Int {
    return roundToRowSpan(minHeight)
}

private fun calculateRowSpan(height: Int): Int {
    for(i in 1 until 5){
        val rowSpan = getRowSpan(i)
        if(rowSpan >= height) return i
    }
    return 5
}

private fun roundToRowSpan(height: Int): Int {
    return getWidgetHeight(calculateRowSpan(height))
}

private fun getRowSpan(spanY: Int): Int {
    return (spanY * resources.dip(74)) - resources.dip(2)
}

private fun getWidgetHeight(spanY: Int): Int {
    val repository = get<ExpandedRepository>(ExpandedRepository::class.java)
    return repository.widgetRowHeight * spanY
}

private fun getWidgetWidth(spanX: Int): Int {
    val repository = get<ExpandedRepository>(ExpandedRepository::class.java)
    return repository.widgetColumnWidth * spanX
}

private fun calculateColumnSpan(width: Int): Int {
    for(i in 1 until 5){
        val rowSpan = getColumnSpan(i)
        if(rowSpan >= width) return i
    }
    return 5
}

private fun roundToColumnSpan(width: Int): Int {
    return getWidgetWidth(calculateColumnSpan(width))
}

private fun getColumnSpan(spanX: Int): Int {
    return (spanX * resources.dip(74)) - resources.dip(2)
}