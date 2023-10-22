package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {

    @Query("select * from `Action` order by `index` asc")
    fun getAll(): Flow<List<Action>>

    @Query("select * from `Action` where id=:id")
    fun getAction(id: String): Flow<Action?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(action: Action)

    @Update
    fun update(action: Action)

    @Delete
    fun delete(action: Action)

}