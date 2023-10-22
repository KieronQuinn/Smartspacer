package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.PendingIntent
import android.app.PendingIntentHidden
import android.content.Intent
import android.os.Build
import dev.rikka.tools.refine.Refine

fun PendingIntent.getIntent(): Intent? {
    return try {
        Refine.unsafeCast<PendingIntentHidden>(this).intent
    }catch (e: Exception){
        //Probably invalid access
        null
    }
}

fun PendingIntent.isActivityCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isActivity
    } else {
        true //Assume worst case
    }
}