package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.content.Context
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.AlarmRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.REQUIREMENT_TIME_DATE
import com.kieronquinn.app.smartspacer.utils.extensions.toDate
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

/**
 *  Time & Date based requirement, to only show providers between start & end times on specific days
 *  of the week. Backed by WorkManager in [AlarmRepository].
 */
class TimeDateRequirement: SmartspacerRequirementProvider() {

    private val dataRepository by inject<DataRepository>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        val data = getData(smartspacerId) ?: return false
        val now = LocalTime.now()
        val day = LocalDateTime.now().dayOfWeek
        return data.days.contains(day) && now.isAfter(data.startTime) && now.isBefore(data.endTime)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val data = smartspacerId?.let { getData(it) }
        val description = if(data == null) {
            provideContext().getString(R.string.requirement_time_date_content_generic)
        }else provideContext().getDescription(data)
        val intent = ConfigurationActivity.createIntent(provideContext(), REQUIREMENT_TIME_DATE)
        return Config(
            provideContext().getString(R.string.requirement_time_date_title),
            description,
            Icon.createWithResource(provideContext(), R.drawable.ic_requirement_time_date),
            setupActivity = intent,
            configActivity = intent
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        dataRepository.deleteRequirementData(smartspacerId)
    }

    override fun createBackup(smartspacerId: String): Backup {
        val data = getData(smartspacerId) ?: return Backup()
        val gson = get<Gson>()
        val label = provideContext().getDescription(data).toString()
        return Backup(gson.toJson(data), label)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val data = try {
            gson.fromJson(backup.data, TimeDateRequirementData::class.java)
        }catch (e: Exception) {
            return false
        }
        dataRepository.updateRequirementData(
            smartspacerId,
            TimeDateRequirementData::class.java,
            RequirementDataType.TIME_DATE,
            ::restoreNotifyChange
        ) {
            TimeDateRequirementData(smartspacerId, data.startTime, data.endTime, data.days)
        }
        return true
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun Context.getDescription(data: TimeDateRequirementData): CharSequence {
        val timeFormat = DateFormat.getTimeFormat(provideContext())
        val startTime = timeFormat.format(data.startTime.atDate(LocalDate.now()).toDate())
        val endTime = timeFormat.format(data.endTime.atDate(LocalDate.now()).toDate())
        return if(data.days.size == DayOfWeek.values().size) {
            getString(R.string.requirement_time_date_content_all_days, startTime, endTime)
        }else{
            val days = data.days.sortedBy { it.ordinal }.joinToString(", ") {
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
            getString(R.string.requirement_time_date_content, startTime, endTime, days)
        }
    }

    private fun getData(smartspacerId: String): TimeDateRequirementData? {
        return dataRepository.getRequirementData(smartspacerId, TimeDateRequirementData::class.java)
    }

    data class TimeDateRequirementData(
        @SerializedName("id")
        val id: String,
        @SerializedName("start_time")
        val startTime: LocalTime = LocalTime.of(9, 0),
        @SerializedName("end_time")
        val endTime: LocalTime = LocalTime.of(17, 0),
        @SerializedName("days")
        val days: Set<DayOfWeek> = DayOfWeek.values().toSet()
    )

}