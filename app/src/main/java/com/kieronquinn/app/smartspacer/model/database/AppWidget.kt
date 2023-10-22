package com.kieronquinn.app.smartspacer.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class AppWidget(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo(name = "owner_package")
    val ownerPackage: String,
    @ColumnInfo(name = "ui_surface")
    var surface: UiSurface,
    @ColumnInfo(name = "tint_colour")
    var tintColour: TintColour,
    @ColumnInfo(name = "multi_page")
    val multiPage: Boolean,
    @ColumnInfo("show_controls")
    val showControls: Boolean
): Parcelable {

    fun cloneWithId(newAppWidgetId: Int): AppWidget {
        return AppWidget(
            newAppWidgetId,
            ownerPackage,
            surface,
            tintColour,
            multiPage,
            showControls
        )
    }

}
