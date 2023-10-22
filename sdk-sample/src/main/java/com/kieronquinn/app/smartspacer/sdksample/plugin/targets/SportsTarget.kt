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
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Target showing a title, subtitle and icon, as well as an example of a two team sports
 *  game with scores
 */
class SportsTarget: SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.HeadToHead(
                context = provideContext(),
                id = "sports",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "sports"),
                title = Text("Sports Target"),
                subtitle = Text("ENG v GER"),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_sports)),
                headToHeadTitle = Text("Final"),
                headToHeadFirstCompetitorText = Text("2"),
                headToHeadSecondCompetitorText = Text("1"),
                headToHeadFirstCompetitorIcon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.flag_england), shouldTint = false),
                headToHeadSecondCompetitorIcon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.flag_germany), shouldTint = false),
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
            "Sports Target",
            "Target showing a title, subtitle and icon, as well as an example of a two " +
                    "team sports game with scores",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_sports)
        )
    }

}