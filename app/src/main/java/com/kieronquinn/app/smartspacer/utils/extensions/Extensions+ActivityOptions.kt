package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.ActivityOptions
import android.os.Bundle

fun ActivityOptions_fromBundle(bundle: Bundle): ActivityOptions {
    return ActivityOptions::class.java.getMethod("fromBundle", Bundle::class.java)
        .invoke(null, bundle) as ActivityOptions
}