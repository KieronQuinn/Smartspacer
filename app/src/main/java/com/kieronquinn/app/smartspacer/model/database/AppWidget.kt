package com.kieronquinn.app.smartspacer.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.TintColour
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import kotlinx.parcelize.Parcelize

/**
 * The type of pinning applied to this widget
 */
enum class PinMode(val labelRes: Int) {
    NONE(R.string.widget_configuration_pin_mode_none),
    PAGE(R.string.widget_configuration_pin_mode_page),
    TARGET(R.string.widget_configuration_pin_mode_target)
}

/**
 * Fallback behavior when a pinned target/page is unavailable
 */
enum class PinnedFallbackBehavior(val labelRes: Int) {
    SHOW_DATE(R.string.widget_configuration_pin_fallback_show_date),
    SHOW_FIRST_TARGET(R.string.widget_configuration_pin_fallback_show_first),
    HIDE_WIDGET(R.string.widget_configuration_pin_fallback_hide)
}

@Entity
@Parcelize
data class AppWidget(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    /**
     *  App which is hosting the widget, typically the launcher. Used to pause/resume via the
     *  accessibility service or the Shizuku service.
     */
    @ColumnInfo(name = "owner_package")
    val ownerPackage: String,
    /**
     *  Which surface the widget is being displayed on, most of the time [UiSurface.HOMESCREEN],
     *  but if the user adds it to an app which isn't their default launcher, Smartspacer will
     *  offer to set it up as [UiSurface.LOCKSCREEN]. This determines which Targets & Complications
     *  to show.
     */
    @ColumnInfo(name = "ui_surface")
    var surface: UiSurface = UiSurface.HOMESCREEN,
    /**
     *  The colour to be applied to this Widget's text & icons
     */
    @ColumnInfo(name = "tint_colour")
    var tintColour: TintColour = TintColour.AUTOMATIC,
    /**
     *  Whether this Widget should allow multiple pages, controlled by the user. Ignored in list
     *  mode.
     */
    @ColumnInfo(name = "multi_page")
    val multiPage: Boolean = true,
    /**
     *  Whether to show the arrow controls in the Widget to change pages. Ignored in list mode.
     */
    @ColumnInfo("show_controls")
    val showControls: Boolean = true,
    /**
     *  Whether to animate page changes in the Widget. Ignored in list mode.
     */
    @ColumnInfo("animate")
    val animate: Boolean = true,
    /**
     *  Whether to show a shadow on text where required. Icons are not supported.
     */
    @ColumnInfo("show_shadow")
    val showShadow: Boolean = true,
    /**
     *  When enabled, the Widget will display in a vertical list rather than horizontal pages.
     *  This loses a number of performance optimisations, including only rendering the current
     *  page and only refreshing as required, instead a full list reload will be required by changes
     */
    @ColumnInfo("list_mode")
    val listMode: Boolean = false,
    /**
     *  Additional padding applied to the left & right of the widget. Operates independently from
     *  the launcher, becomes `dp` at runtime.
     */
    @ColumnInfo("padding")
    val padding: Int = 0,
    /**
     *  When enabled, the controls will become invisible. They're still there, they still work,
     *  they're just invisible.
     */
    @ColumnInfo("hide_controls")
    val hideControls: Boolean = false,
    /**
     * Whether the widget is MaterialYou styled
     */
    @ColumnInfo("materialyou_styled")
    val materialYouStyled: Boolean = false,
    /**
     * The type of pinning applied to this widget
     */
    @ColumnInfo("pin_mode")
    val pinMode: PinMode = PinMode.NONE,
    /**
     * The page index this widget is pinned to (used when pinMode = PAGE)
     */
    @ColumnInfo("pinned_page_index")
    val pinnedPageIndex: Int = 0,
    /**
     * The ID of the target this widget is pinned to (used when pinMode = TARGET)
     */
    @ColumnInfo("pinned_target_id")
    val pinnedTargetId: String? = null,
    /**
     * Fallback behavior when the pinned target/page is unavailable
     */
    @ColumnInfo("pinned_fallback_behavior")
    val pinnedFallbackBehavior: PinnedFallbackBehavior = PinnedFallbackBehavior.SHOW_DATE,
): Parcelable {

    fun cloneWithId(newAppWidgetId: Int): AppWidget {
        return copy(appWidgetId = newAppWidgetId)
    }

    fun equalsForUi(other: Any?): Boolean {
        if(other !is AppWidget) return false
        if(other.appWidgetId != appWidgetId) return false
        if(other.ownerPackage != ownerPackage) return false
        if(other.tintColour != tintColour) return false
        if(other.multiPage != multiPage) return false
        if(other.showControls != showControls) return false
        if(other.listMode != listMode) return false
        if(other.hideControls != hideControls) return false
        if(other.animate != animate) return false
        if(other.showShadow != showShadow) return false
        if(other.materialYouStyled != materialYouStyled) return false
        if(other.pinMode != pinMode) return false
        if(other.pinnedPageIndex != pinnedPageIndex) return false
        if(other.pinnedTargetId != pinnedTargetId) return false
        if(other.pinnedFallbackBehavior != pinnedFallbackBehavior) return false
        return true
    }

}
