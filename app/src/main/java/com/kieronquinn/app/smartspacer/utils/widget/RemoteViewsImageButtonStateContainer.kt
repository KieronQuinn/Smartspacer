package com.kieronquinn.app.smartspacer.utils.widget

import androidx.annotation.IdRes

/**
 *  Alternative to alpha & colour filters for states of buttons due to Android bug where it can't
 *  handle them correctly in nested RemoteViews.
 *
 *  Takes the ID of a ViewFlipper (parent), icon states for disabled
 */
class RemoteViewsStateContainer(
    @IdRes val viewFlipper: Int
) {
}