package com.kieronquinn.app.smartspacer.model.appshortcuts

import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState

data class AppShortcut(
    val title: CharSequence,
    val icon: AppShortcutIcon,
    val packageName: String,
    val shortcutId: String
): ExpandedState.BaseShortcut(ItemType.APP_SHORTCUT) {

    override fun equals(other: Any?): Boolean {
        if(other !is AppShortcut) return false
        if(other.title != title) return false
        if(other.packageName != packageName) return false
        if(other.shortcutId != shortcutId) return false
        //Icons are not compared due to the parcel descriptor
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + shortcutId.hashCode()
        return result
    }

}
