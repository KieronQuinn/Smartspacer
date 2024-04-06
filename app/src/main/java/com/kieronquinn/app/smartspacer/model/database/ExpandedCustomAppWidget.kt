package com.kieronquinn.app.smartspacer.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class ExpandedCustomAppWidget(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int?,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "index")
    var index: Int,
    @ColumnInfo(name = "span_x")
    val spanX: Int,
    @ColumnInfo(name = "span_y")
    val spanY: Int,
    @ColumnInfo(name = "show_when_locked")
    val showWhenLocked: Boolean,
    @ColumnInfo(name = "round_corners")
    val roundCorners: Boolean,
    @ColumnInfo(name = "full_width")
    val fullWidth: Boolean
)
