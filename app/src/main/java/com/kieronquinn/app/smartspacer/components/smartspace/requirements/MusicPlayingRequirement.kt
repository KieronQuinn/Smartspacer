package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.content.Intent
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.MediaRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.activities.permission.notification.NotificationPermissionActivity
import org.koin.android.ext.android.inject

class MusicPlayingRequirement: SmartspacerRequirementProvider() {

    private val mediaRepository by inject<MediaRepository>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        return mediaRepository.mediaController.value != null
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.requirement_music_playing_label),
            resources.getString(R.string.requirement_music_playing_content),
            Icon.createWithResource(provideContext(), R.drawable.ic_target_music),
            setupActivity = Intent(provideContext(), NotificationPermissionActivity::class.java)
        )
    }

}