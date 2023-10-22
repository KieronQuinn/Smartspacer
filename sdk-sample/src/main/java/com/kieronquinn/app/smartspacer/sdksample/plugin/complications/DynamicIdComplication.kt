package com.kieronquinn.app.smartspacer.sdksample.plugin.complications

import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.SampleConfigurationActivity
import com.kieronquinn.app.smartspacer.sdksample.plugin.SampleSetupActivity
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Basic complication which supports being added to Smartspacer multiple times, displaying the
 *  complication ID as the content.
 */
class DynamicIdComplication: SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOf(
            ComplicationTemplate.Basic(
                id = "dynamic_id_$smartspacerId",
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_dynamic_id)),
                content = Text(smartspacerId),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.android.vending"
                })
            ).create()
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        //Use this to remove the ID's config from your data
    }

    override fun getConfig(smartspacerId: String?): Config {
        val description = if(smartspacerId != null){
            "Complication displaying the complication ID $smartspacerId"
        }else{
            "Complication which displays the Smartspacer ID for the complication"
        }
        return Config(
            "Dynamic ID Complication",
            description,
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_dynamic_id),
            allowAddingMoreThanOnce = true,
            configActivity = Intent(provideContext(), SampleConfigurationActivity::class.java),
            setupActivity = Intent(provideContext(), SampleSetupActivity::class.java),
            refreshPeriodMinutes = 3
        )
    }

}