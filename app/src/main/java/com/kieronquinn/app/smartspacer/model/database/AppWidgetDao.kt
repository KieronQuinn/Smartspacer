package com.kieronquinn.app.smartspacer.model.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppWidgetDao {

    @Query("select * from `AppWidget`")
    fun getAll(): Flow<List<AppWidget>>

    @Query("select * from `AppWidget` where app_widget_id=:id")
    fun getById(id: Int): Flow<AppWidget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(widget: AppWidget)

    @Update
    fun update(widget: AppWidget)

    @Delete
    fun delete(widget: AppWidget)

}