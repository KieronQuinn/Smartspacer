package com.kieronquinn.app.smartspacer.utils.remoteviews

import android.os.Build
import android.widget.RemoteViews
import android.widget.RemoteViewsHidden

/**
 *  Wrapper for [RemoteViews] which prevents the setting of the collection flag. This breaks the
 *  logic which on Android < 13 makes it impossible to set multiple click listeners per widget list
 *  item row. It unfortunately also means we cannot use template/fill intents anymore, but it's
 *  the only way.
 *
 *  This should only be used as the root level node for the list adapter, since it is not required
 *  anywhere else.
 *
 *  Patched in Android 16, so now only used on Android < 13. Android >= 13 uses the recommended
 *  method.
 */
class FlagDisabledRemoteViews: RemoteViewsHidden {

    constructor(landscape: RemoteViews, portrait: RemoteViews): super(landscape, portrait)
    constructor(remoteViews: RemoteViews): super(remoteViews)

    companion object {
        private const val FLAG_WIDGET_IS_COLLECTION_CHILD = 2
    }

    override fun addFlags(flags: Int) {
        if(flags == FLAG_WIDGET_IS_COLLECTION_CHILD &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        super.addFlags(flags)
    }

}