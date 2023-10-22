package com.kieronquinn.app.smartspacer.utils

import android.appwidget.AppWidgetManager
import io.mockk.every
import io.mockk.mockkStatic

fun appWidgetManager(manager: AppWidgetManager): AppWidgetManager {
    mockkStatic(AppWidgetManager::class).apply {
        every { AppWidgetManager.getInstance(any()) } returns manager
    }
    return manager
}