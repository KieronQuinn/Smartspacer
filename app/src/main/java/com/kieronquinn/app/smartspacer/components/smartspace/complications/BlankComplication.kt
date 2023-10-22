package com.kieronquinn.app.smartspacer.components.smartspace.complications

import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.utils.extensions.Icon_createEmptyIcon
import android.graphics.drawable.Icon as AndroidIcon

class BlankComplication: SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOf(
            ComplicationTemplate.Basic(
                "blank_$smartspacerId",
                Icon(Icon_createEmptyIcon()),
                Text(" "),
                null
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.complication_blank_title),
            resources.getString(R.string.complication_blank_content),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_blank),
            allowAddingMoreThanOnce = true
        )
    }

}