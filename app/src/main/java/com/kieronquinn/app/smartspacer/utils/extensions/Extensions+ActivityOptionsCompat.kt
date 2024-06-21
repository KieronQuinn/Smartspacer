package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.ActivityOptions
import android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
import android.os.Build
import androidx.core.app.ActivityOptionsCompat

fun ActivityOptionsCompat.allowBackground() = apply {
    if (Build.VERSION.SDK_INT >= 34) {
        val impl = Class
            .forName("androidx.core.app.ActivityOptionsCompat\$ActivityOptionsCompatImpl")
        val inner = impl.getDeclaredField("mActivityOptions").apply {
            isAccessible = true
        }.get(this) as ActivityOptions
        inner.apply {
            pendingIntentBackgroundActivityStartMode = MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
    }
}