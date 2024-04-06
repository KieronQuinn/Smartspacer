package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfoHidden
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import dev.rikka.tools.refine.Refine
import kotlin.math.floor

var AppWidgetProviderInfo.providerInfo: ActivityInfo
    get() {
        return Refine.unsafeCast<AppWidgetProviderInfoHidden>(this).providerInfo
    }
    set(value) {
        Refine.unsafeCast<AppWidgetProviderInfoHidden>(this).providerInfo = value
    }

@SuppressLint("NewApi")
fun AppWidgetProviderInfo.loadPreview(): RemoteViews? {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || previewLayout == 0) return null
    return RemoteViews(provider.packageName, previewLayout)
}

/**
 *  Target sizes become unreliable for smaller widgets due to our custom grid size. Only consider
 *  them for larger widgets.
 */
private fun AppWidgetProviderInfo.canUseTargetSizes(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        targetCellWidth > 1 && targetCellHeight > 1
    } else false
}

@SuppressLint("NewApi")
fun AppWidgetProviderInfo.getWidthSpan(columnWidth: Int): Int {
    val target = if (canUseTargetSizes()) {
        targetCellWidth
    }else null
    return (target ?: getSpan(minWidth, columnWidth)).coerceAtMost(5)
}

@SuppressLint("NewApi")
fun AppWidgetProviderInfo.getHeightSpan(rowHeight: Int): Int {
    val target = if (canUseTargetSizes()) {
        targetCellHeight
    }else null
    return (target ?: getSpan(minHeight, rowHeight)).coerceAtMost(5)
}

fun AppWidgetProviderInfo.isFourByOne(columnWidth: Int, rowHeight: Int): Boolean {
    return getWidthSpan(columnWidth) == 4 && getHeightSpan(rowHeight) == 1
}

fun AppWidgetProviderInfo.getWidth(context: Context, availableWidth: Int, columnWidth: Int): Int {
    return getWidgetWidth(context, getWidthSpan(columnWidth), availableWidth)
}

fun AppWidgetProviderInfo.getHeight(context: Context, availableWidth: Int, rowHeight: Int): Int {
    return getWidgetHeight(context, getHeightSpan(rowHeight), availableWidth)
}

private fun getWidgetHeight(context: Context, spanY: Int, availableWidth: Int): Int {
    val rowHeight = context.getWidgetRowHeight(availableWidth)
    return rowHeight * spanY
}

private fun getWidgetWidth(context: Context, spanX: Int, availableWidth: Int): Int {
    val columnWidth = context.getWidgetColumnWidth(availableWidth)
    return columnWidth * spanX
}

const val WIDGET_MIN_COLUMNS = 5

fun Context.getWidgetColumnWidth(availableWidth: Int): Int {
    val maxWidth = resources
        .getDimension(R.dimen.expanded_smartspace_widget_column_max_width)
    return (availableWidth / WIDGET_MIN_COLUMNS.toFloat())
        .coerceAtMost(maxWidth).toInt()
}

fun Context.getWidgetRowHeight(availableWidth: Int): Int {
    return (getWidgetColumnWidth(availableWidth) * 1.4f).toInt()
}

fun Context.getWidgetColumnCount(availableWidth: Int): Int {
    val width = getWidgetColumnWidth(availableWidth)
    return floor(availableWidth / width.toFloat()).toInt()
}

private fun getSpan(minSize: Int, spanSize: Int): Int {
    for (i in 1..30) {
        val span = spanSize * i
        if (span > minSize) {
            return i
        }
    }
    return 1
}