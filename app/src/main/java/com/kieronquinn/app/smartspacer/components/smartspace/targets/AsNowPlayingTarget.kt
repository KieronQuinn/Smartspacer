package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.smartspace.notifications.AsNowPlayingTargetNotification
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import com.kieronquinn.app.smartspacer.utils.extensions.resolveService

/**
 *  Now Playing Target for Android System Intelligence
 */
class AsNowPlayingTarget: NowPlayingTarget() {

    companion object {
        const val PACKAGE_NAME = "com.google.android.as"
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.target.asnowplaying"
    }

    override val targetPrefix = "as_now_playing"
    override val packageName = PACKAGE_NAME

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_now_playing_label),
            description = resources.getString(R.string.target_now_playing_description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_now_playing),
            setupActivity = Intent(provideContext(), NotificationPermissionActivity::class.java),
            compatibilityState = getCompatibilityState(),
            notificationProvider = AsNowPlayingTargetNotification.AUTHORITY,
            allowAddingMoreThanOnce = true
        )
    }

    /**
     *  Checks for the existence of the Ambient Music service, which is only present in Pixel builds
     *  of Android System Intelligence. Simply checking for ASI isn't enough as OEM builds use the
     *  same package name but do not contain Now Playing.
     */
    private fun isNowPlayingInstalled(): Boolean {
        val serviceComponent = ComponentName(
            PACKAGE_NAME,
            "com.google.intelligence.sense.ambientmusic.AmbientMusicDetector\$Service"
        )
        val serviceIntent = Intent().apply {
            component = serviceComponent
        }
        return provideContext().packageManager.resolveService(serviceIntent) != null
    }

    /**
     *  Checks for the existence of new Now Playing, which if there is a good indicator that
     *  notifications will not be shown and the Target will not work.
     */
    private fun isNewNowPlayingInstalled(): Boolean {
        return provideContext().packageManager
            .isPackageInstalled("com.google.android.apps.pixel.nowplaying")
    }

    /**
     *  Checks for the existence of Public Compute Services, which is able to hook and re-enable
     *  the notification, making the Target work again
     */
    private fun isPCSInstalled(): Boolean {
        return provideContext().packageManager
            .isPackageInstalled("com.google.android.apps.pixel.nowplaying")
    }

    private fun getCompatibilityState(): CompatibilityState {
        return when {
            !isNowPlayingInstalled() -> CompatibilityState.Incompatible(
                resources.getString(R.string.target_now_playing_description_unavailable)
            )
            isNewNowPlayingInstalled() && !isPCSInstalled() -> CompatibilityState.Incompatible(
                resources.getString(R.string.target_now_playing_description_unavailable_pixel)
            )
            else -> CompatibilityState.Compatible
        }
    }

}