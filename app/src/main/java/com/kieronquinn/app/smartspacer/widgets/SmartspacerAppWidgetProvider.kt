package com.kieronquinn.app.smartspacer.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartspacerAppWidgetProvider: AppWidgetProvider(), KoinComponent {

    private val appWidgetRepository by inject<AppWidgetRepository>()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetRepository.updateWidget(id)
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

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetRepository.updateWidget(appWidgetId)
    }

}