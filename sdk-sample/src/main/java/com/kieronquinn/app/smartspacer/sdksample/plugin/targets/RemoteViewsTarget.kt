package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.widget.RemoteViews
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
 *  Custom layout Target with a fallback for unsupported spaces
 */
@SuppressLint("RemoteViewLayout")
class RemoteViewsTarget : SmartspacerTargetProvider() {

    private val targets by lazy {
        listOf(
            TargetTemplate.RemoteViews(
                RemoteViews(provideContext().packageName, R.layout.remoteviews),
                TargetTemplate.Basic(
                    id = "remoteviews_target",
                    componentName = ComponentName(BuildConfig.APPLICATION_ID, "remoteviews_target"),
                    title = Text("RemoteViews Target"),
                    subtitle = Text("You should put a fallback here for if RemoteViews are not supported"),
                    icon = Icon(
                        AndroidIcon.createWithResource(
                            BuildConfig.APPLICATION_ID,
                            R.drawable.ic_target_remoteviews
                        )
                    ),
                    onClick = TapAction(intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        `package` = "com.google.android.calculator"
                    })
                )
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
            "RemoteViews Target",
            "Custom layout Target with a fallback for unsupported spaces",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_remoteviews)
        )
    }

}