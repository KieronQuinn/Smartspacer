package com.kieronquinn.app.smartspacer.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Target(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "authority")
    val authority: String,
    @ColumnInfo(name = "index")
    var index: Int,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "any_requirements")
    var anyRequirements: Set<String> = emptySet(),
    @ColumnInfo(name = "all_requirements")
    var allRequirements: Set<String> = emptySet(),
    @ColumnInfo(name = "show_on_home_screen")
    var showOnHomeScreen: Boolean = true,
    @ColumnInfo(name = "show_on_lock_screen")
    var showOnLockScreen: Boolean = true,
    @ColumnInfo(name = "show_on_expanded")
    var showOnExpanded: Boolean = true,
    @ColumnInfo(name = "show_on_music")
    var showOnMusic: Boolean = false,
    @ColumnInfo(name = "show_remote_views")
    var showRemoteViews: Boolean = true,
    @ColumnInfo(name = "show_widget")
    var showWidget: Boolean = true,
    @ColumnInfo(name = "show_shortcuts")
    var showShortcuts: Boolean = true,
    @ColumnInfo(name = "show_app_shortcuts")
    var showAppShortcuts: Boolean = true,
    @ColumnInfo(name = "expanded_show_when_locked")
    var expandedShowWhenLocked: Boolean = true
): Parcelable {

    fun hasRequirement(id: String): Boolean {
        return anyRequirements.contains(id) || allRequirements.contains(id)
    }

}