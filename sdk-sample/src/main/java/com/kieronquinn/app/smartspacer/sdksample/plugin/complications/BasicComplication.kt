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
 *  Simple, basic complication showing an icon and some text. This complication has a configuration
 *  activity that can be launched from the Smartspacer settings UI, as well as a setup activity
 *  which will be launched during the complication being added. The user must press a button to
 *  continue setup, cancelling will not add the complication.
 */
class BasicComplication: SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOf(
            ComplicationTemplate.Basic(
                id = "basic",
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_basic)),
                content = Text("Hello World!"),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.android.vending"
                })
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            "Basic Complication",
            "Simple, basic complication showing an icon and some text",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_basic),
            configActivity = Intent(provideContext(), SampleConfigurationActivity::class.java),
            setupActivity = Intent(provideContext(), SampleSetupActivity::class.java),
            refreshPeriodMinutes = 3
        )
    }

}