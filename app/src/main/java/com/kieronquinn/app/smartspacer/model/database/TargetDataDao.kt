package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDataDao {

    @Query("select * from `TargetData`")
    fun getAll(): Flow<List<TargetData>>

    @Query("select * from `TargetData` where id=:id")
    fun getById(id: String): TargetData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: TargetData)

    @Update
    fun update(data: TargetData)

    @Delete
    fun delete(data: TargetData)

}