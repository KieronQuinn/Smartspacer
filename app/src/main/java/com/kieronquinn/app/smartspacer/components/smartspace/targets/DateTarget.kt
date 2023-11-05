package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.graphics.drawable.Icon
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BasicTemplateData
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import java.util.UUID

class DateTarget: SmartspacerTargetProvider() {

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return listOf(
            SmartspaceTarget(
                smartspaceTargetId = UUID.randomUUID().toString(),
                featureType = SmartspaceTarget.FEATURE_WEATHER,
                baseAction = SmartspaceAction(id = "", title = ""),
                componentName = ComponentName(provideContext(), MainActivity::class.java),
                //In the event there's no targets or complications, a date will be shown anyway
                hideIfNoComplications = true,
                templateData = BasicTemplateData()
            ).apply {
                canBeDismissed = false
            }
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        //This target cannot be dismissed
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.target_date_label),
            description = resources.getString(R.string.target_date_description),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_target_date),
            allowAddingMoreThanOnce = true
        )
    }

}