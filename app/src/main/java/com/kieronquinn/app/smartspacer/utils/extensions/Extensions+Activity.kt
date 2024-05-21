package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity

@SuppressLint("SoonBlockedPrivateApi")
fun Activity.setIsChangingConfigurations(isChangingConfigurations: Boolean) {
    Activity::class.java.getDeclaredField("mChangingConfigurations").apply {
        isAccessible = true
    }.set(this, isChangingConfigurations)
}