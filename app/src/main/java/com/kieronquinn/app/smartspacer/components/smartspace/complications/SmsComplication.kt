package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.content.Intent
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmsRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.ui.activities.permission.sms.SmsPermissionActivity
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class SmsComplication: SmartspacerComplicationProvider() {

    private val smsRepository by inject<SmsRepository>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val unreadCount = smsRepository.smsUnreadCount.value
        if(unreadCount == 0) return emptyList()
        val icon = if(smsRepository.isSmsAppGoogleMessages()){
            R.drawable.ic_complication_sms_google
        }else{
            R.drawable.ic_complication_sms
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        return listOf(ComplicationTemplate.Basic(
            "sms_$smartspacerId",
            icon = Icon(AndroidIcon.createWithResource(provideContext(), icon)),
            content = Text(unreadCount.toString()),
            onClick = TapAction(intent = intent)
        ).create())
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.complication_sms_label),
            description = resources.getString(R.string.complication_sms_description),
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_complication_sms),
            setupActivity = Intent(provideContext(), SmsPermissionActivity::class.java),
            allowAddingMoreThanOnce = true
        )
    }

}