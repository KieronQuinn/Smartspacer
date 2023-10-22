package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.content.Context
import android.graphics.drawable.Icon
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository
import com.kieronquinn.app.smartspacer.repositories.WiFiRepository.WiFiNetwork
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class WiFiRequirement: SmartspacerRequirementProvider() {

    private val dataRepository by inject<DataRepository>()
    private val wifiRepository by inject<WiFiRepository>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        val settings = getData(smartspacerId) ?: RequirementData()
        return if(settings.allowUnconnected) {
            wifiRepository.availableNetworks.value.any {
                it.matches(settings.ssid, settings.macAddress)
            }
        } else {
            wifiRepository.connectedNetwork.value
                ?.matches(settings.ssid, settings.macAddress) == true
        }
    }

    override fun getConfig(smartspacerId: String?): Config {
        val data = smartspacerId?.let {
            getData(smartspacerId)
        }
        val description = data?.let {
            provideContext().getDescription(it)
        } ?: resources.getString(R.string.requirement_wifi_content)
        val configIntent = ConfigurationActivity.createIntent(
            provideContext(), NavGraphMapping.REQUIREMENT_WIFI
        )
        return Config(
            label = resources.getString(R.string.requirement_wifi_title),
            description = description,
            icon = Icon.createWithResource(provideContext(), R.drawable.ic_requirement_wifi),
            configActivity = configIntent,
            setupActivity = configIntent
        )
    }

    override fun createBackup(smartspacerId: String): Backup {
        val data = getData(smartspacerId) ?: return Backup()
        val gson = get<Gson>()
        val label = provideContext().getDescription(data)
        return Backup(gson.toJson(data), label)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val data = try {
            gson.fromJson(backup.data, RequirementData::class.java)
        }catch (e: Exception) {
            return false
        } ?: return false
        dataRepository.updateRequirementData(
            smartspacerId,
            RequirementData::class.java,
            RequirementDataType.WIFI,
            ::restoreNotifyChange
        ) {
            RequirementData(data.ssid, data.macAddress, data.allowUnconnected)
        }
        return false //Launch the settings UI to force location opt-in if needed
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getData(smartspacerId: String): RequirementData? {
        return dataRepository.getRequirementData(smartspacerId, RequirementData::class.java)
    }

    private fun Context.getDescription(data: RequirementData): String {
        val first = if(data.allowUnconnected) {
            getString(R.string.requirement_wifi_content_in_range_or_connected_to)
        }else{
            getString(R.string.requirement_wifi_content_connected_to)
        }
        val second = when {
            data.ssid != null && data.macAddress != null -> {
                getString(R.string.requirement_wifi_content_ssid_mac, data.ssid, data.macAddress)
            }
            data.ssid != null -> data.ssid
            data.macAddress != null -> data.macAddress
            else -> return getString(R.string.requirement_wifi_content)
        }
        return getString(R.string.requirement_wifi_content_base, first, second)
    }

    private fun WiFiNetwork.matches(ssid: String?, mac: String?): Boolean {
        if(ssid == null && mac == null) return true
        if(ssid != null && this.ssid != ssid) return false
        if(mac != null && this.mac != mac) return false
        return true
    }

    data class RequirementData(
        @SerializedName("ssid")
        val ssid: String? = null,
        @SerializedName("mac_address")
        val macAddress: String? = null,
        @SerializedName("allow_unconnected")
        val allowUnconnected: Boolean = true
    )

}