package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.ActivityOptions
import android.app.PendingIntent
import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun PendingIntent.sendSafely() {
    return try {
        if (Build.VERSION.SDK_INT >= 34) {
            //Allow background launches
            send(getActivityOptions().toBundle())
        } else {
            send()
        }
    }catch (e: PendingIntent.CanceledException){
        //Sending was cancelled
    }
}

private fun getActivityOptions(): ActivityOptions {
    return ActivityOptions.makeBasic().apply {
        if (Build.VERSION.SDK_INT >= 34) {
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
    }
}