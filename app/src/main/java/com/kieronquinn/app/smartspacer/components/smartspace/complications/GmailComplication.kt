package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.GmailRepository
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.utils.gmail.GmailContract
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class GmailComplication: SmartspacerComplicationProvider() {

    private val dataRepository by inject<DataRepository>()
    private val gmailRepository by inject<GmailRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val unreadCount = gmailRepository.getUnreadCount(smartspacerId)
        if(unreadCount == 0) return emptyList()
        val label = unreadCount?.toString()
            ?: resources.getString(R.string.complication_gmail_unknown)
        val intent = unreadCount?.let {
            provideContext().packageManager.getLaunchIntentForPackage(GmailContract.PACKAGE)
        } ?: ConfigurationActivity.createIntent(
            provideContext(), NavGraphMapping.COMPLICATION_GMAIL
        ).apply {
            putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, smartspacerId)
        }
        return listOf(
            ComplicationTemplate.Basic(
                "gmail_$smartspacerId",
                content = Text(label),
                icon = Icon(
                    AndroidIcon.createWithResource(provideContext(), R.drawable.ic_complication_gmail)
                ),
                onClick = TapAction(intent = intent)
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        val settings = if(smartspacerId != null){
            getSettings(smartspacerId)
        } else null
        return Config(
            label = resources.getString(R.string.complication_gmail_label),
            description = getDescription(settings),
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_complication_gmail),
            allowAddingMoreThanOnce = true,
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), NavGraphMapping.COMPLICATION_GMAIL
            ),
            setupActivity = ConfigurationActivity.createIntent(
                provideContext(), NavGraphMapping.COMPLICATION_GMAIL
            )
        )
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = getSettings(smartspacerId)
        return Backup(gson.toJson(settings), getDescription(settings))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val settings = gson.fromJson(backup.data ?: return false, ActionData::class.java)
        dataRepository.updateActionData(
            smartspacerId,
            ActionData::class.java,
            ActionDataType.GMAIL,
            ::onRestored
        ) {
            val data = it ?: ActionData(smartspacerId)
            data.copy(id = smartspacerId, enabledLabels = settings.enabledLabels)
        }
        return false //Show settings UI to grant permission
    }

    private fun onRestored(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getDescription(settings: ActionData?): String {
        return if(settings?.accountName != null && settings.enabledLabels.isNotEmpty()){
            resources.getQuantityString(
                R.plurals.complication_gmail_description_filled,
                settings.enabledLabels.size,
                settings.accountName,
                settings.enabledLabels.size
            )
        }else resources.getString(R.string.complication_gmail_description)
    }

    private fun getSettings(smartspacerId: String): ActionData {
        return dataRepository.getActionData(smartspacerId, ActionData::class.java)
            ?: ActionData(smartspacerId)
    }

    data class ActionData(
        @SerializedName("id")
        val id: String,
        @SerializedName("account_name")
        val accountName: String? = null,
        @SerializedName("enabled_labels")
        val enabledLabels: Set<String> = emptySet()
    )

}