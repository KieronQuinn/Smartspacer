package com.kieronquinn.app.smartspacer.model.expanded

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import java.util.UUID

/** Controls what is shown on each tab button in the bottom nav bar. */
enum class NavItemDisplayMode(@StringRes val label: Int) {
    /** Show text label only — no icon on any tab (default). */
    LABEL_ONLY(R.string.expanded_tab_settings_mode_label),
    /** Show icon only — no text on any tab. */
    ICON_ONLY(R.string.expanded_tab_settings_mode_icon),
    /** Show icon on selected tab, text label on unselected tabs. */
    ICON_AND_LABEL(R.string.expanded_tab_settings_mode_icon_label);

    companion object {
        val DEFAULT = ICON_AND_LABEL
    }
}

/**
 * Represents one tab in the expanded view bottom navigation bar.
 * Each tab is mapped to a specific AppWidget instance (by [appWidgetId]) so the user
 * can switch between differently-configured widget instances (e.g. different ReadYou feeds).
 */
data class ExpandedTabConfig(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("label")
    val label: String,
    @SerializedName("app_widget_id")
    val appWidgetId: Int,
    /** Unicode codepoint of the user-chosen Material Symbols icon. Null = no icon. */
    @SerializedName("icon_codepoint")
    val iconCodepoint: Int? = null
)
