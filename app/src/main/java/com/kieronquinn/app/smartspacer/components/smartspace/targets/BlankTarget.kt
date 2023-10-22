package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.Companion.createIntent
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.utils.extensions.Icon_createEmptyIcon
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class BlankTarget: SmartspacerTargetProvider() {

    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val data = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: TargetData()
        return listOf(TargetTemplate.Basic(
            "blank_$smartspacerId",
            componentName = ComponentName(provideContext(), BlankTarget::class.java),
            featureType = SmartspaceTarget.FEATURE_UNDEFINED,
            title = Text(""),
            subtitle = Text(""),
            icon = Icon(Icon_createEmptyIcon()),
            onClick = null,
            subComplication = data.createSubComplicationOrNull(smartspacerId)
        ).create().apply {
            canBeDismissed = false
            canTakeTwoComplications = data.showComplications
            hideIfNoComplications = data.showComplications && data.hideIfNoComplications
        })
    }

    private fun TargetData.createSubComplicationOrNull(smartspacerId: String): SmartspaceAction? {
        if(showComplications) return null
        return ComplicationTemplate.Basic(
            "blank_$smartspacerId",
            Icon(Icon_createEmptyIcon()),
            Text(""),
            null
        ).create()
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.target_blank_title),
            resources.getString(R.string.target_blank_content),
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_blank),
            allowAddingMoreThanOnce = true,
            configActivity = createIntent(provideContext(), NavGraphMapping.TARGET_BLANK)
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    override fun createBackup(smartspacerId: String): Backup {
        val data = dataRepository.getTargetData(smartspacerId, TargetData::class.java)
            ?: TargetData()
        return Backup(gson.toJson(data), resources.getString(R.string.target_blank_content))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val targetData = try {
            gson.fromJson(backup.data, TargetData::class.java)
        }catch (e: Exception){
            null
        } ?: return false
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.BLANK,
            ::onUpdate
        ) {
            targetData
        }
        return true
    }

    private fun onUpdate(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    data class TargetData(
        @SerializedName("show_complications")
        val showComplications: Boolean = false,
        @SerializedName("hide_if_no_complications")
        val hideIfNoComplications: Boolean = false
    )

}