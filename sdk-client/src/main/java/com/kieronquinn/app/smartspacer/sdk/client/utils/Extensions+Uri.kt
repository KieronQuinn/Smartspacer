package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.net.Uri
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Uri.isLoadable(): Boolean {
    return authority == "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.proxyprovider"
}