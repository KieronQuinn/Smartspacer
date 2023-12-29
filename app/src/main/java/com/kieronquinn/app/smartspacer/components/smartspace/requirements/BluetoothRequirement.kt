package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.BluetoothRepository
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.NotificationRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.model.CompatibilityState
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.screens.configuration.bluetooth.BluetoothRequirementConfigurationFragment.Companion.getIntent
import com.kieronquinn.app.smartspacer.utils.extensions.getNameOrNull
import org.koin.android.ext.android.inject

class BluetoothRequirement: SmartspacerRequirementProvider() {

    private val bluetoothRepository by inject<BluetoothRepository>()
    private val dataRepository by inject<DataRepository>()
    private val notificationRepository by inject<NotificationRepository>()
    private val gson by inject<Gson>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        val name = getRequirementData(smartspacerId)?.name ?: return false
        if(!bluetoothRepository.hasPermission.value ||
            !bluetoothRepository.hasBackgroundPermission.value) {
            showErrorNotification()
            return false
        }
        notificationRepository.cancelNotification(NotificationId.BLUETOOTH_REQUIRED)
        return bluetoothRepository.connectedDevices.value.any {
            it.getNameOrNull() == name
        }
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            resources.getString(R.string.requirement_bluetooth_title),
            getDescription(smartspacerId),
            Icon.createWithResource(provideContext(), R.drawable.ic_bluetooth),
            compatibilityState = getCompatibilityState(),
            setupActivity = getIntent(provideContext(), true),
            configActivity = getIntent(provideContext(), false)
        )
    }

    override fun createBackup(smartspacerId: String): Backup {
        val data = getRequirementData(smartspacerId) ?: return Backup()
        return Backup(gson.toJson(data), getDescription(smartspacerId))
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val data = try {
            gson.fromJson(backup.data, RequirementData::class.java)
        }catch (e: Exception) {
            null
        } ?: return false
        dataRepository.updateRequirementData(
            smartspacerId,
            RequirementData::class.java,
            RequirementDataType.BLUETOOTH,
            ::onUpdated
        ) {
            RequirementData(name = data.name)
        }
        return false //Always open setup to check permissions
    }

    private fun onUpdated(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getCompatibilityState(): CompatibilityState {
        return if(!bluetoothRepository.isCompatible) {
            CompatibilityState.Incompatible(
                resources.getString(R.string.requirement_bluetooth_incompatible)
            )
        }else CompatibilityState.Compatible
    }

    private fun getDescription(smartspacerId: String?): String {
        val data = smartspacerId?.let { getRequirementData(it) }
        return if(data?.name != null) {
            resources.getString(R.string.requirement_bluetooth_description_set, data.name)
        }else{
            resources.getString(R.string.requirement_bluetooth_description)
        }
    }

    private fun getRequirementData(smartspacerId: String): RequirementData? {
        return dataRepository.getRequirementData(smartspacerId, RequirementData::class.java)
    }

    private fun showErrorNotification() = with(provideContext()) {
        notificationRepository.showNotification(
            NotificationId.BLUETOOTH_REQUIRED,
            NotificationChannel.ERROR
        ) {
            val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            it.setContentTitle(getString(R.string.requirement_bluetooth_notification_title))
            it.setContentText(getString(R.string.requirement_bluetooth_notification_content))
            it.setSmallIcon(R.drawable.ic_warning)
            it.setAutoCancel(false)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.BLUETOOTH_REQUIRED.ordinal,
                    appInfoIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.requirement_bluetooth_notification_title))
        }
    }

    data class RequirementData(
        @SerializedName("name")
        val name: String? = null
    )

}