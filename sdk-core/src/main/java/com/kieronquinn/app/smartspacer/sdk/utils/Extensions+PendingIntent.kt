package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.PendingIntent
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun PendingIntent.sendSafely() {
    return try {
        send()
    }catch (e: PendingIntent.CanceledException){
        //Sending was cancelled
    }
}