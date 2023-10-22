package com.kieronquinn.app.smartspacer.sdk.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val INTENT_KEY_SECURITY_TAG = "security_tag"
@RestrictTo(RestrictTo.Scope.LIBRARY)
const val PENDING_INTENT_REQUEST_CODE = 999
@RestrictTo(RestrictTo.Scope.LIBRARY)
const val EXTRA_EXCLUDE_FROM_SMARTSPACER = "exclude_from_smartspacer"

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Intent.applySecurity(context: Context) {
    val securityTag = PendingIntent.getActivity(
        context,
        PENDING_INTENT_REQUEST_CODE,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE
    )
    putExtra(INTENT_KEY_SECURITY_TAG, securityTag)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Intent.shouldExcludeFromSmartspacer(): Boolean {
    return getBooleanExtra(EXTRA_EXCLUDE_FROM_SMARTSPACER, false)
}