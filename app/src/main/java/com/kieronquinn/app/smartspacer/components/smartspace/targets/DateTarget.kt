package com.kieronquinn.app.smartspacer.components.smartspace.targets

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.icu.text.DateFormat
import android.icu.text.DisplayContext
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
import java.util.Date
import java.util.UUID


class DateTarget: SmartspacerTargetProvider() {

    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val dateFormat = getDateFormat(smartspacerId) ?: getDefaultDateFormat()
        val featureType = SmartspaceTarget.FEATURE_UNDEFINED
        val header = SmartspaceAction(
            id = "",
            icon = null,
            title = dateFormat.format(ZonedDateTime.now()),
            intent = getCalendarIntent()
        )
        val templateData = BasicTemplateData(
            primaryItem = SubItemInfo(
                text = Text(dateFormat.format(ZonedDateTime.now())),
                tapAction = TapAction(intent = getCalendarIntent())
            )
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

    private fun getDateFormat(smartspacerId: String): Formatter? {
        return dataRepository.getTargetData(smartspacerId, TargetData::class.java)?.dateFormat?.let {
            try {
                DateTimeFormatter.ofPattern(it)
            }catch (e: IllegalArgumentException) {
                null
            }
        }?.let {
            Formatter.DateTimeFormatter(it)
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

    private fun getDefaultDateFormat(): Formatter.DateFormat {
        return DateFormat.getInstanceForSkeleton("EEEMMMd").apply {
            setContext(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE)
        }.let {
            Formatter.DateFormat(it)
        }
    }

    sealed class Formatter {
        data class DateTimeFormatter(val formatter: java.time.format.DateTimeFormatter): Formatter() {
            override fun format(date: ZonedDateTime): String {
                return formatter.format(date)
            }
        }
        data class DateFormat(val formatter: android.icu.text.DateFormat): Formatter() {
            override fun format(date: ZonedDateTime): String {
                return formatter.format(Date.from(date.toInstant()))
            }
        }

        abstract fun format(date: ZonedDateTime): String
    }

    data class TargetData(
        @SerializedName("date_format")
        val dateFormat: String? = null
    )

}