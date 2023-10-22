package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.content.Intent
import android.provider.CallLog
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.CallsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.ui.activities.permission.calllog.CallLogPermissionActivity
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class MissedCallsComplication: SmartspacerComplicationProvider() {

    private val callsRepository by inject<CallsRepository>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val missedCallsCount = callsRepository.missedCallsCount.value
        if(missedCallsCount == 0) return emptyList()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = CallLog.Calls.CONTENT_URI
        }
        return listOf(
            ComplicationTemplate.Basic(
                "missed_calls_$smartspacerId",
                Icon(AndroidIcon.createWithResource(
                    provideContext(), R.drawable.ic_complication_missed_calls
                )),
                Text(missedCallsCount.toString()),
                TapAction(intent = intent)
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.complication_missed_calls_label),
            resources.getString(R.string.complication_missed_calls_description),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_complication_missed_calls),
            setupActivity = Intent(provideContext(), CallLogPermissionActivity::class.java)
        )
    }

}