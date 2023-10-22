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
import com.kieronquinn.app.smartspacer.sdksample.plugin.SampleConfigurationActivity
import com.kieronquinn.app.smartspacer.sdksample.plugin.SampleSetupActivity
import android.graphics.drawable.Icon as AndroicIcon

/**
 *  Basic target which supports being added to Smartspacer multiple times, displaying the target ID
 *  as the title.
 */
class DynamicIdTarget: SmartspacerTargetProvider() {

    private val dismissedIds = mutableSetOf<String>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        if(dismissedIds.contains(smartspacerId)) return emptyList()
        return getTarget(smartspacerId)
    }

    private fun getTarget(smartspacerId: String): List<SmartspaceTarget> {
        return listOf(
            TargetTemplate.Basic(
                id = "dynamic_id_target_$smartspacerId",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "dynamic_id_target"),
                title = Text(smartspacerId),
                subtitle = Text("Dynamic ID target"),
                icon = Icon(AndroicIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_dynamic_id)),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ).create()
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        dismissedIds.add(smartspacerId)
        notifyChange()
        return true
    }

    override fun onProviderRemoved(smartspacerId: String) {
        dismissedIds.remove(smartspacerId)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val description = if(smartspacerId != null){
            "Target displaying the target ID $smartspacerId"
        }else{
            "Target which displays the Smartspacer ID for the target"
        }
        return Config(
            "Dynamic ID Target",
            description,
            AndroicIcon.createWithResource(provideContext(), R.drawable.ic_target_dynamic_id),
            allowAddingMoreThanOnce = true,
            configActivity = Intent(provideContext(), SampleConfigurationActivity::class.java),
            setupActivity = Intent(provideContext(), SampleSetupActivity::class.java)
        )
    }

}