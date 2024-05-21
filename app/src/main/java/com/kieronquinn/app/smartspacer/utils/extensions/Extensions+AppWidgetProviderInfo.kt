package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetProviderInfoHidden
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.kieronquinn.app.smartspacer.R
import dev.rikka.tools.refine.Refine
import org.koin.java.KoinJavaComponent.inject
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

private val batteryRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.battery_recommendations)
}

private val conversationsRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.conversations_recommendations)
}

private val noteTakingRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.note_taking_recommendations)
}

private val snapshotRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.snapshot_recommendations)
}

private val weatherRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.weather_recommendations)
}

private val fitnessRecommendations by lazy {
    val context by inject<Context>(Context::class.java)
    context.resources.getStringArray(R.array.fitness_recommendations)
}

fun AppWidgetProviderInfo.getCategory(): WidgetCategory {
    val component = provider.flattenToString()
    if(batteryRecommendations.contains(component)) {
        return WidgetCategory.BATTERY
    }
    if(conversationsRecommendations.contains(component)) {
        return WidgetCategory.CONVERSATIONS
    }
    if(noteTakingRecommendations.contains(component)) {
        return WidgetCategory.NOTE_TAKING
    }
    if(snapshotRecommendations.contains(component)) {
        return WidgetCategory.SNAPSHOT
    }
    if(weatherRecommendations.contains(component)) {
        return WidgetCategory.WEATHER
    }
    if(fitnessRecommendations.contains(component)) {
        return WidgetCategory.FITNESS
    }
    val category = providerInfo.applicationInfo.category
    return when(category) {
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE -> WidgetCategory.ENTERTAINMENT
        ApplicationInfo.CATEGORY_SOCIAL -> WidgetCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> WidgetCategory.NEWS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> WidgetCategory.PRODUCTIVITY
        else -> WidgetCategory.OTHERS
    }
}

enum class WidgetCategory(@StringRes val labelRes: Int) {
    BATTERY(R.string.battery_widget_recommendation_category_label),
    CONVERSATIONS(R.string.conversations_widget_recommendation_category_label),
    NOTE_TAKING(R.string.note_taking_widget_recommendation_category_label),
    SNAPSHOT(R.string.snapshot_widget_recommendation_category_label),
    PRODUCTIVITY(R.string.productivity_widget_recommendation_category_label),
    WEATHER(R.string.weather_widget_recommendation_category_label),
    NEWS(R.string.news_widget_recommendation_category_label),
    FITNESS(R.string.fitness_widget_recommendation_category_label),
    OTHERS(R.string.others_widget_recommendation_category_label),
    SOCIAL(R.string.social_widget_recommendation_category_label),
    ENTERTAINMENT(R.string.entertainment_widget_recommendation_category_label),
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