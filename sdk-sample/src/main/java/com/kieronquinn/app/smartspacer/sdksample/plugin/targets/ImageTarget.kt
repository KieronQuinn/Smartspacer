package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import android.graphics.drawable.Icon as AndroicIcon

/**
 *  Shows a title, short description, icon and large rectangular bitmap image (used for a map)
 */
class ImageTarget: SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.Image(
                provideContext(),
                id = "image_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "image_target"),
                title = Text("Image Target"),
                subtitle = Text("Large rectangular image"),
                icon = Icon(AndroicIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_image)),
                image = Icon(AndroicIcon.createWithResource(context, R.drawable.map_commute)),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
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
            "Image Target",
            "Shows a title, short description, icon and large rectangular bitmap image",
            AndroicIcon.createWithResource(provideContext(), R.drawable.ic_target_image)
        )
    }

}