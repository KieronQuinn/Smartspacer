package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpandedCustomAppWidgetDao {

    @Query("select * from `ExpandedCustomAppWidget` order by `index`")
    fun getAll(): Flow<List<ExpandedCustomAppWidget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(widget: ExpandedCustomAppWidget)

    @Update
    fun update(widget: ExpandedCustomAppWidget)

    @Query("delete from `ExpandedCustomAppWidget` where app_widget_id=:id")
    fun delete(id: Int)

}