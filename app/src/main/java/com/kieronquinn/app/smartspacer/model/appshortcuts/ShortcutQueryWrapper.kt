package com.kieronquinn.app.smartspacer.model.appshortcuts

import android.content.pm.ShortcutQueryWrapper
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShortcutQueryWrapper(
    val queryFlags: Int
): Parcelable {

    fun toSystemShortcutQueryWrapper(): ShortcutQueryWrapper {
        return ShortcutQueryWrapper().apply {
            queryFlags = this@ShortcutQueryWrapper.queryFlags
        }
    }

}