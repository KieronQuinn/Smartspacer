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
 *  Shows a title, subtitle, icon and a card prompt with text, program name and an image
 */
class LoyaltyCardTarget: SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.LoyaltyCard(
                context = provideContext(),
                id = "shopping_card_prompt_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "shopping_card_prompt"),
                title = Text("Loyalty Card"),
                subtitle = Text("Your loyalty card"),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_loyalty_card), shouldTint = false),
                cardIcon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.qr_code), shouldTint = false),
                cardPrompt = Text("Loyalty Card"),
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
            "Loyalty Card Target",
            "Shows a title, subtitle, icon and a card prompt with text and an image",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_loyalty_card)
        )
    }

}