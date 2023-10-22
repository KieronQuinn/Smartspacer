package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDataDao {

    @Query("select * from `ActionData`")
    fun getAll(): Flow<List<ActionData>>

    @Query("select * from `ActionData` where id=:id")
    fun getById(id: String): ActionData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: ActionData)

    @Update
    fun update(data: ActionData)

    @Delete
    fun delete(data: ActionData)

}