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
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Target showing a title, subtitle, icon and large button-style view with icon and text. This
 *  target has a configuration activity that can be launched from the Smartspacer settings UI, as
 *  well as a setup activity which will be launched during the target being added. The user must
 *  press a button to continue setup, cancelling will not add the target.
 */
class ButtonTarget: SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.Button(
                context = provideContext(),
                id = "button_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "button_target"),
                title = Text("Button Target"),
                subtitle = Text("Click the button!"),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_button)),
                buttonText = Text("Click me!"),
                buttonIcon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_button)),
                onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = "com.google.android.calculator"
                })
            ).create().apply {
                isSensitive = true
            }
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
            "Button Target",
            "Shows a title, subtitle, icon and large button-style view with icon and text",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_button),
            configActivity = Intent(provideContext(), SampleConfigurationActivity::class.java),
            setupActivity = Intent(provideContext(), SampleSetupActivity::class.java)
        )
    }

}