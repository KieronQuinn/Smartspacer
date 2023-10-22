package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RequirementDao {

    @Query("select * from `Requirement`")
    fun getAll(): Flow<List<Requirement>>

    @Query("select * from `Requirement` where id=:id")
    fun getById(id: String): Flow<Requirement?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(requirement: Requirement)

    @Update
    fun update(requirement: Requirement)

    @Delete
    fun delete(requirement: Requirement)

}