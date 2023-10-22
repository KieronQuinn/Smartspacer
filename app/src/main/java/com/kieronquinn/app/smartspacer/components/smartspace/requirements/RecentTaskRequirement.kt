package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.content.Context
import android.graphics.drawable.Icon
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.*
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import org.koin.android.ext.android.inject

class RecentTaskRequirement: SmartspacerRequirementProvider() {

    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val dataRepository by inject<DataRepository>()
    private val recentTaskRepository by inject<RecentTasksRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val gson by inject<Gson>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        if(showShizukuNotificationIfNeeded()) return false
        val settings = getSettings(smartspacerId)
        if(settings?.appPackageName == null) return false
        val apps = recentTaskRepository.recentTaskPackages.value.let {
            if(settings.limit != null) it.take(settings.limit) else it
        }
        return apps.contains(settings.appPackageName)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val settings = smartspacerId?.let {
            getSettings(it)
        } ?: RequirementData()
        return Config(
            label = resources.getString(R.string.requirement_recent_apps_label),
            description = getDescription(settings),
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_requirement_recent_task),
            compatibilityState = getCompatibilityState(),
            configActivity = ConfigurationActivity.createIntent(
                provideContext(), NavGraphMapping.REQUIREMENT_RECENT_TASK
            )
        )
    }

    override fun createBackup(smartspacerId: String): Backup {
        val settings = getSettings(smartspacerId) ?: return Backup()
        return Backup(gson.toJson(settings), getDescription(settings))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val settings = gson.fromJson(backup.data ?: return false, RequirementData::class.java)
        dataRepository.updateRequirementData(
            smartspacerId,
            RequirementData::class.java,
            RequirementDataType.RECENT_TASK,
            ::onBackupRestored
        ) {
            val data = it ?: RequirementData()
            data.copy(appPackageName = settings.appPackageName, limit = settings.limit)
        }
        return true
    }

    private fun onBackupRestored(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getDescription(settings: RequirementData): String {
        val label = settings.appPackageName?.let {
            provideContext().packageManager.getPackageLabel(it)
        } ?: return resources.getString(R.string.requirement_recent_apps_description)
        return if(settings.limit != null){
            resources.getString(
                R.string.requirement_recent_apps_description_filled_with_limit,
                label,
                settings.limit
            )
        }else{
            resources.getString(R.string.requirement_recent_apps_description_filled, label)
        }
    }

    private fun getCompatibilityState(): CompatibilityState {
        return when {
            !settingsRepository.enhancedMode.getSync() -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(
                        R.string.requirement_recent_apps_description_unsupported
                    )
                )
            }
            else -> CompatibilityState.Compatible
        }
    }

    private fun getSettings(smartspacerId: String): RequirementData? {
        return dataRepository.getRequirementData(smartspacerId, RequirementData::class.java)
    }

    private fun showShizukuNotificationIfNeeded(): Boolean {
        if(!shizukuServiceRepository.isReady.value && settingsRepository.enhancedMode.getSync()){
            notificationRepository.showShizukuNotification(
                R.string.notification_shizuku_content_recent_task
            )
            return true
        }else{
            notificationRepository.cancelNotification(NotificationId.SHIZUKU)
        }
        return false
    }

    data class RequirementData(
        @SerializedName("app_package_name")
        val appPackageName: String? = null,
        @SerializedName("limit")
        val limit: Int? = null
    )

}