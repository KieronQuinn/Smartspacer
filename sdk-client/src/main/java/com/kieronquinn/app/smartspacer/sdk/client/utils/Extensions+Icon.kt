package com.kieronquinn.app.smartspacer.sdk.client.utils

import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Checks if the [Icon] can be loaded, since only Uri icons run through the proxy can be loaded.
 *  It *is* possible for non-proxied Uris to be loaded, but apps would need to allow all launchers,
 *  so it's limited to just proxied Uris.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun Icon.isLoadable(): Boolean {
    return when(icon.type){
        AndroidIcon.TYPE_URI, AndroidIcon.TYPE_URI_ADAPTIVE_BITMAP -> {
            icon.uri.isLoadable()
        }
        else -> true
    }
}