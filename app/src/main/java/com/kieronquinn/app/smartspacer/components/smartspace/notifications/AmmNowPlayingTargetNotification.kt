package com.kieronquinn.app.smartspacer.components.smartspace.notifications

import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AmmNowPlayingTarget

class AmmNowPlayingTargetNotification: NowPlayingTargetNotification() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.notifications.ammnowplaying"
    }

    override val packageName = AmmNowPlayingTarget.PACKAGE_NAME
    override val targetClass = AmmNowPlayingTarget::class.java

}