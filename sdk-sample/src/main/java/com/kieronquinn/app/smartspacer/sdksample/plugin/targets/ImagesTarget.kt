package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.content.ComponentName
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.ExampleLocalImageProvider
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Target showing a title, subtitle, icon and images shown in a sequence
 */
class ImagesTarget: SmartspacerTargetProvider() {

    private val items by lazy {
        val uris = ExampleLocalImageProvider.getUris(100)
        uris.map {
            AndroidIcon.createWithContentUri(it)
        }.map {
            Icon(it, shouldTint = false)
        }
    }

    private val targets by lazy {
        listOf(
            TargetTemplate.Images(
                context = provideContext(),
                id = "images_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "images_target"),
                title = Text("Images Target"),
                subtitle = Text("Shows images in a sequence"),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_image)),
                images = items,
                frameDurationMs = 1000,
                onClick = null
            ).create()
        ).toMutableList()
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return targets
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        targets.clear()
        notifyChange()
        return true
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            "Images Target",
            "Shows a title, subtitle, icon and images shown in a sequence",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_image),
            notificationProvider = "${BuildConfig.APPLICATION_ID}.notifications.example",
            broadcastProvider = "${BuildConfig.APPLICATION_ID}.broadcasts.example"
        )
    }

}