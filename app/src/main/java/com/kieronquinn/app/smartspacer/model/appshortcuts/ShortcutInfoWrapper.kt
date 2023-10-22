package com.kieronquinn.app.smartspacer.model.appshortcuts

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShortcutInfoWrapper(
    val shortcutInfo: ShortcutInfo,
    val icon: Icon? = null,
    val iconDescriptor: ParcelFileDescriptor? = null
): Parcelable
