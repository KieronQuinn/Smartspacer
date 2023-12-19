package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.CalendarContract
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.TargetDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BasicTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.Companion.createIntent
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import org.koin.android.ext.android.inject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class DateTarget: SmartspacerTargetProvider() {

    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val dateFormat = getDateFormat(smartspacerId)
        val featureType = if(dateFormat != null) {
            SmartspaceTarget.FEATURE_UNDEFINED
        }else{
            SmartspaceTarget.FEATURE_WEATHER
        }
        val header = if(dateFormat != null) {
            SmartspaceAction(
                id = "",
                icon = null,
                title = dateFormat.format(ZonedDateTime.now()),
                intent = getCalendarIntent()
            )
        }else null
        val templateData = BasicTemplateData(
            primaryItem = if(dateFormat != null) {
                SubItemInfo(
                    text = Text(dateFormat.format(ZonedDateTime.now())),
                    tapAction = TapAction(intent = getCalendarIntent())
                )
            }else null
        )
        return listOf(
            SmartspaceTarget(
                smartspaceTargetId = UUID.randomUUID().toString(),
                featureType = featureType,
                headerAction = header,
                baseAction = SmartspaceAction(id = "", title = ""),
                componentName = ComponentName(provideContext(), MainActivity::class.java),
                //In the event there's no targets or complications, a date will be shown anyway
                hideIfNoComplications = true,
                templateData = templateData
            ).apply {
                canBeDismissed = false
                canTakeTwoComplications = true
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
            allowAddingMoreThanOnce = true,
            configActivity = createIntent(provideContext(), NavGraphMapping.TARGET_DATE)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        super.onProviderRemoved(smartspacerId)
        dataRepository.deleteTargetData(smartspacerId)
    }

    override fun createBackup(smartspacerId: String): Backup {
        val description = resources.getString(R.string.target_date_description)
        val data = dataRepository.getTargetData(smartspacerId, TargetData::class.java)?.let {
            gson.toJson(it)
        }
        return Backup(data, description)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val data = try {
            gson.fromJson(backup.data, TargetData::class.java)
        }catch (e: Exception) {
            null
        } ?: return false
        dataRepository.updateTargetData(
            smartspacerId,
            TargetData::class.java,
            TargetDataType.DATE,
            ::onChanged
        ) {
            data
        }
        return true
    }

    private fun onChanged(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getDateFormat(smartspacerId: String): DateTimeFormatter? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)?.let {
            try {
                DateTimeFormatter.ofPattern(it.dateFormat)
            }catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    private fun getCalendarIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.appendId(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time"),
                System.currentTimeMillis()
            ).build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    data class TargetData(
        @SerializedName("date_format")
        val dateFormat: String? = null
    )

}