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
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.REQUIREMENT_APP_PREDICTION
import com.kieronquinn.app.smartspacer.utils.extensions.getPackageLabel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class AppPredictionRequirement: SmartspacerRequirementProvider() {

    private val appPrediction by inject<AppPredictionRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val shizukuServiceRepository by inject<ShizukuServiceRepository>()
    private val settingsRepository by inject<SmartspacerSettingsRepository>()
    private val dataRepository by inject<DataRepository>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        if(showShizukuNotificationIfNeeded()) return false
        val data = getData(smartspacerId) ?: return false
        return appPrediction.appPredictions.value.contains(data.packageName)
    }

    private fun showShizukuNotificationIfNeeded(): Boolean {
        if(!shizukuServiceRepository.isReady.value && settingsRepository.enhancedMode.getSync()){
            notificationRepository.showShizukuNotification(
                R.string.notification_shizuku_content_app_prediction
            )
            return true
        }else{
            notificationRepository.cancelNotification(NotificationId.SHIZUKU)
        }
        return false
    }

    override fun getConfig(smartspacerId: String?): Config {
        val data = smartspacerId?.let { getData(it) }
        val description = if(data == null){
            provideContext().getString(R.string.requirement_app_prediction_content_generic)
        }else{
            provideContext().getString(R.string.requirement_app_prediction_content, data.getPackageLabel())
        }
        val intent = ConfigurationActivity.createIntent(
            provideContext(), REQUIREMENT_APP_PREDICTION
        )
        return Config(
            provideContext().getString(R.string.requirement_app_prediction),
            description,
            Icon.createWithResource(provideContext(), R.drawable.ic_requirement_app_prediction),
            compatibilityState = getCompatibilityState(),
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
        val label = provideContext().getString(
            R.string.requirement_app_prediction_content, data.getPackageLabel()
        )
        return Backup(gson.toJson(data), label)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val data = try {
            gson.fromJson(backup.data, AppPredictionRequirementData::class.java)
        }catch (e: Exception) {
            return false
        }
        dataRepository.updateRequirementData(
            smartspacerId,
            AppPredictionRequirementData::class.java,
            RequirementDataType.APP_PREDICTION,
            ::restoreNotifyChange
        ) {
            AppPredictionRequirementData(smartspacerId, data.packageName)
        }
        return true
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun AppPredictionRequirementData.getPackageLabel(): CharSequence {
        return provideContext().packageManager.getPackageLabel(packageName)
            ?: provideContext().getString(R.string.requirement_app_prediction_generic_label)
    }

    private fun getData(smartspacerId: String): AppPredictionRequirementData? {
        return dataRepository.getRequirementData(
            smartspacerId, AppPredictionRequirementData::class.java
        )
    }

    private fun getCompatibilityState(): CompatibilityState {
        return when {
            !appPrediction.isSupported() -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.requirement_app_prediction_incompatible)
                )
            }
            !settingsRepository.enhancedMode.getSync() -> {
                CompatibilityState.Incompatible(
                    provideContext().getString(R.string.requirement_app_prediction_enhanced)
                )
            }
            else -> CompatibilityState.Compatible
        }
    }

    data class AppPredictionRequirementData(
        @SerializedName("id")
        val id: String,
        @SerializedName("package_name")
        val packageName: String
    )

}