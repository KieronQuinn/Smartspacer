package com.kieronquinn.app.smartspacer.model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationListenerDao {

    @Query("select * from `NotificationListener`")
    fun getAll(): Flow<List<NotificationListener>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(notification: NotificationListener)

    @Query("delete from `NotificationListener` where id=:id")
    fun delete(id: String)

}