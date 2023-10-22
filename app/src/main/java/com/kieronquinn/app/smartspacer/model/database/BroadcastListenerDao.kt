package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BroadcastListenerDao {

    @Query("select * from `BroadcastListener`")
    fun getAll(): Flow<List<BroadcastListener>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(broadcast: BroadcastListener)

    @Query("delete from `BroadcastListener` where id=:id")
    fun delete(id: String)

}