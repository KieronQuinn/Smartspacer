package com.kieronquinn.app.smartspacer.components.smartspace.notifications

import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.components.smartspace.targets.AsNowPlayingTarget

class AsNowPlayingTargetNotification: NowPlayingTargetNotification() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.notifications.asnowplaying"
    }

    override val packageName = AsNowPlayingTarget.PACKAGE_NAME
    override val targetClass = AsNowPlayingTarget::class.java

}