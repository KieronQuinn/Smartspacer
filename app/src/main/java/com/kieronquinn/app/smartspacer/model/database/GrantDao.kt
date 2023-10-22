package com.kieronquinn.app.smartspacer.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GrantDao {

    @Query("select * from `Grant` where package_name=:packageName")
    fun getGrantForPackage(packageName: String): Grant?

    @Query("select * from `Grant`")
    fun getAll(): Flow<List<Grant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addGrant(grant: Grant)

}