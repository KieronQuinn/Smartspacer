package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.AmmNowPlayingTargetNotification
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled

/**
 *  Now Playing Target for Ambient Music Mod
 */
class AmmNowPlayingTarget: NowPlayingTarget() {

    companion object {
        const val PACKAGE_NAME = "com.kieronquinn.app.pixelambientmusic"
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.ammnowplaying"
    }

    override val targetPrefix = "amm_now_playing"
    override val packageName = PACKAGE_NAME

    override fun getExpandedState(expandedState: ExpandedState): ExpandedState {
        return expandedState.copy(
            appShortcuts = ExpandedState.AppShortcuts(setOf(packageName))
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_ambient_music_mod_label),
            description = resources.getString(R.string.target_ambient_music_mod_description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_now_playing),
            setupActivity = Intent(provideContext(), NotificationPermissionActivity::class.java),
            compatibilityState = getCompatibilityState(),
            notificationProvider = AmmNowPlayingTargetNotification.AUTHORITY
        )
    }

    private fun isAmbientMusicModInstalled(): Boolean {
        return provideContext().packageManager.isPackageInstalled(PACKAGE_NAME)
    }

    private fun getCompatibilityState(): CompatibilityState {
        return if(isAmbientMusicModInstalled()){
            CompatibilityState.Compatible
        }else{
            CompatibilityState.Incompatible(
                resources.getString(R.string.target_ambient_music_mod_description_unavailable)
            )
        }
    }

}