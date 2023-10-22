package com.kieronquinn.app.smartspacer.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BroadcastListener(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "authority")
    val authority: String
)