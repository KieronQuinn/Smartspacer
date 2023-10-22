package com.kieronquinn.app.smartspacer.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Grant(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    /**
     *  Whether the [packageName] can bind widgets via Smartspacer
     */
    @ColumnInfo(name = "widget")
    var widget: Boolean = false,
    /**
     *  Whether the [packageName] can access Smartspace directly as a launcher
     */
    @ColumnInfo(name = "smartspace")
    var smartspace: Boolean = false,
    /**
     *  Whether the [packageName] should receive OEM smartspace events, and be able to access the
     *  icon ContentProvider
     */
    @ColumnInfo(name = "oem_smartspace")
    var oemSmartspace: Boolean = false,
    /**
     *  Whether the [packageName] can register a Notification Listener
     */
    @ColumnInfo(name = "notifications")
    var notifications: Boolean = false
): Parcelable
