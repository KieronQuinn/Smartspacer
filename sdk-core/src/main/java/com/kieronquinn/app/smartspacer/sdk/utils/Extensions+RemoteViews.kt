package com.kieronquinn.app.smartspacer.sdk.utils

import android.widget.RemoteViews

fun RemoteViews.copy(): RemoteViews {
    return RemoteViews(this)
}