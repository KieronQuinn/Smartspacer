package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.content.Context
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.sdk.client.R

fun RemoteViews.wrap(context: Context, darkText: Boolean): RemoteViews {
    return if(darkText) {
        RemoteViews(context.packageName, R.layout.remoteviews_wrapper_light)
    }else{
        RemoteViews(context.packageName, R.layout.remoteviews_wrapper_dark)
    }.apply {
        removeAllViews(R.id.remoteviews_wrapper)
        addView(R.id.remoteviews_wrapper, this@wrap)
    }
}