package com.kieronquinn.app.smartspacer.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
class TargetData(
    @PrimaryKey
    @ColumnInfo(name = "id")
    override val id: String,
    @ColumnInfo(name = "type")
    override val type: String,
    @ColumnInfo(name = "data")
    override val data: String
) : Parcelable, BaseData

enum class TargetDataType {
    MUSIC, NOTIFICATION, CALENDAR, DEFAULT, GREETING, BLANK, DATE, FLASHLIGHT, WIDGET
}