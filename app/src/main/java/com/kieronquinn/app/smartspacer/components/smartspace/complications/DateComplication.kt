package com.kieronquinn.app.smartspacer.components.smartspace.complications

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.ActionDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.sdk.annotations.DisablingTrim
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TrimToFit
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.Companion.createIntent
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import org.koin.android.ext.android.inject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.graphics.drawable.Icon as AndroidIcon

class DateComplication: SmartspacerComplicationProvider() {

    private val dataRepository by inject<DataRepository>()
    private val gson by inject<Gson>()

    @OptIn(DisablingTrim::class)
    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val date = getDateFormat(smartspacerId)?.format(ZonedDateTime.now())
            ?: DateFormat.getMediumDateFormat(provideContext()).format(Calendar.getInstance().time)
        return listOf(
            ComplicationTemplate.Basic(
                id = "date_${smartspacerId}_at_${System.currentTimeMillis()}",
                icon = Icon(AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_calendar)),
                content = Text(date),
                onClick = TapAction(intent = getCalendarIntent()),
                trimToFit = TrimToFit.Disabled
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.complication_date_label),
            description = resources.getString(R.string.complication_date_description),
            icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_calendar),
            allowAddingMoreThanOnce = true,
            configActivity = createIntent(provideContext(), NavGraphMapping.COMPLICATION_DATE)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        super.onProviderRemoved(smartspacerId)
        dataRepository.deleteActionData(smartspacerId)
    }

    override fun createBackup(smartspacerId: String): Backup {
        val description = resources.getString(R.string.complication_date_description)
        val data = dataRepository.getActionData(smartspacerId, ComplicationData::class.java)?.let {
            gson.toJson(it)
        }
        return Backup(data, description)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val data = try {
            gson.fromJson(backup.data, ComplicationData::class.java)
        }catch (e: Exception) {
            null
        } ?: return false
        dataRepository.updateActionData(
            smartspacerId,
            ComplicationData::class.java,
            ActionDataType.DATE,
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
        return dataRepository.getActionData(smartspacerId, ComplicationData::class.java)?.let {
            try {
                DateTimeFormatter.ofPattern(it.dateFormat ?: return null)
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

    data class ComplicationData(
        @SerializedName("date_format")
        val dateFormat: String? = null
    )

}