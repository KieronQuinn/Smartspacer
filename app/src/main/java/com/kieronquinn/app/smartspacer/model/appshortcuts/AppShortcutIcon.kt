package com.kieronquinn.app.smartspacer.model.appshortcuts

import android.graphics.drawable.Icon
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppShortcutIcon(val icon: Icon? = null, val descriptor: ParcelFileDescriptor? = null): Parcelable