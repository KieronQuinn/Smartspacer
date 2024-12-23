package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.os.Build
import androidx.core.os.BuildCompat.isAtLeastPreReleaseCodename

fun isAtLeastU(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}

@SuppressLint("RestrictedApi")
fun isAtLeastBaklava(): Boolean =
    Build.VERSION.SDK_INT >= 36 ||
            (Build.VERSION.SDK_INT >= 35 &&
                    isAtLeastPreReleaseCodename("Baklava", Build.VERSION.CODENAME))