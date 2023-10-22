package com.kieronquinn.app.smartspacer.components.smartspace.requirements

import android.content.Context
import android.graphics.drawable.Icon
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.maps.android.SphericalUtil
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.repositories.DataRepository
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepository
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity
import com.kieronquinn.app.smartspacer.ui.activities.configuration.ConfigurationActivity.NavGraphMapping.REQUIREMENT_GEOFENCE
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class GeofenceRequirement: SmartspacerRequirementProvider() {

    private val geofenceRepository by inject<GeofenceRepository>()
    private val dataRepository by inject<DataRepository>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        val data = getData(smartspacerId) ?: return false
        val lastLocation = geofenceRepository.lastLocation ?: return false
        val distance = SphericalUtil.computeDistanceBetween(data.getLatLng(), lastLocation)
        return distance <= data.radius
    }

    override fun getConfig(smartspacerId: String?): Config {
        val data = smartspacerId?.let { getData(it) }
        val description = if(data == null){
            provideContext().getString(R.string.requirement_geofence_content_generic)
        }else{
            provideContext().getString(R.string.requirement_geofence_content, data.getBestName())
        }
        val intent = ConfigurationActivity.createIntent(provideContext(), REQUIREMENT_GEOFENCE)
        return Config(
            provideContext().getString(R.string.requirement_geofence),
            description,
            Icon.createWithResource(provideContext(), R.drawable.ic_requirement_geofence),
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
            R.string.requirement_geofence_content, data.getBestName()
        )
        return Backup(gson.toJson(data), label)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val gson = get<Gson>()
        val data = try {
            gson.fromJson(backup.data, GeofenceRequirementData::class.java)
        }catch (e: Exception) {
            return false
        }
        dataRepository.updateRequirementData(
            smartspacerId,
            GeofenceRequirementData::class.java,
            RequirementDataType.GEOFENCE,
            ::restoreNotifyChange
        ) {
            GeofenceRequirementData(
                smartspacerId,
                data.latitude,
                data.longitude,
                data.radius,
                data.name,
                data.notificationResponsiveness,
                data.loiteringDelay
            )
        }
        return false //Launch the settings UI to force location opt-in if needed
    }

    private fun restoreNotifyChange(context: Context, smartspacerId: String) {
        notifyChange(smartspacerId)
    }

    private fun getData(smartspacerId: String): GeofenceRequirementData? {
        return dataRepository.getRequirementData(smartspacerId, GeofenceRequirementData::class.java)
    }

    data class GeofenceRequirementData(
        @SerializedName("id")
        val id: String,
        @SerializedName("latitude")
        var latitude: Double,
        @SerializedName("longitude")
        var longitude: Double,
        @SerializedName("radius")
        var radius: Float,
        @SerializedName("name")
        var name: String,
        @SerializedName("notification_responsiveness")
        var notificationResponsiveness: Int,
        @SerializedName("loitering_delay")
        var loiteringDelay: Int
    ) {

        fun getBestName(): String {
            return name.ifBlank { "$latitude, $longitude" }
        }

        fun getLatLng(): LatLng {
            return LatLng(latitude, longitude)
        }

        fun getGeofenceRequest() = Geofence.Builder().apply {
            setCircularRegion(latitude, longitude, radius)
            setRequestId(id)
            if(loiteringDelay != 0) {
                setLoiteringDelay(loiteringDelay)
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
            }else{
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            }
            setNotificationResponsiveness(notificationResponsiveness)
            setExpirationDuration(Geofence.NEVER_EXPIRE)
        }.build()

        override fun equals(other: Any?): Boolean {
            return false
        }

    }

}