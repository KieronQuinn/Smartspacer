package com.kieronquinn.app.smartspacer.sdk.client.utils

import android.R
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
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

/**
 *  Native Smartspace has an enabled state for its icons, but we can't do that as we need to use
 *  the double shadow ImageView. Instead, we manually load Drawables and set the enabled state
 *  if available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun AndroidIcon.getEnabledDrawableOrNull(context: Context): Drawable? {
    if(type != AndroidIcon.TYPE_RESOURCE) return null
    val drawable = loadDrawable(context) ?: return null
    if(drawable !is StateListDrawable) return null
    val index = drawable.findStateDrawableIndex(intArrayOf(-R.attr.state_enabled))
    if(index < 0) return null
    return drawable.getStateDrawable(index)
}