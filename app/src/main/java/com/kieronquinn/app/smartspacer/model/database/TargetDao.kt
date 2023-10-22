package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {

    @Query("select * from `Target` order by `index` asc")
    fun getAll(): Flow<List<Target>>

    @Query("select * from `Target` where id=:id")
    fun getTarget(id: String): Flow<Target?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(target: Target)

    @Update
    fun update(target: Target)

    @Delete
    fun delete(target: Target)

}