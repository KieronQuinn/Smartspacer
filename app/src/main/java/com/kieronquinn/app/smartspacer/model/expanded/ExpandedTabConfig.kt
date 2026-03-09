package com.kieronquinn.app.smartspacer.model.expanded

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Represents one tab in the expanded view bottom navigation bar.
 * Each tab is mapped to a specific AppWidget instance (by [appWidgetId]) so the user
 * can switch between differently-configured widget instances (e.g. different ReadYou feeds).
 */
data class ExpandedTabConfig(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("label")
    val label: String,
    @SerializedName("app_widget_id")
    val appWidgetId: Int
)
