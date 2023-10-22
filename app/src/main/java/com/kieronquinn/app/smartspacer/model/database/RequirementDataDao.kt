package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RequirementDataDao {

    @Query("select * from `RequirementData`")
    fun getAll(): Flow<List<RequirementData>>

    @Query("select * from `RequirementData` where id=:id")
    fun getById(id: String): RequirementData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: RequirementData)

    @Update
    fun update(data: RequirementData)

    @Delete
    fun delete(data: RequirementData)

}