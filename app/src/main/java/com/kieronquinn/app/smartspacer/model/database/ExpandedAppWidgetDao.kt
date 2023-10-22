package com.kieronquinn.app.smartspacer.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpandedAppWidgetDao {

    @Query("select * from `ExpandedAppWidget`")
    fun getAll(): Flow<List<ExpandedAppWidget>>

    @Insert
    fun setExpandedAppWidget(widget: ExpandedAppWidget)

    @Query("delete from `ExpandedAppWidget` where app_widget_id=:id")
    fun delete(id: Int)

}