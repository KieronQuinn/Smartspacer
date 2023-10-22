package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {

    @Query("select * from `Widget`")
    fun getAll(): Flow<List<Widget>>

    @Query("select * from `Widget` where id=:id and type=:type")
    fun getWidget(id: String, type: Widget.Type): Flow<Widget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(widget: Widget)

    @Update
    fun update(widget: Widget)

    @Delete
    fun delete(widget: Widget)

}