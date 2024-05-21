package com.kieronquinn.app.smartspacer.repositories

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Address
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.components.notifications.NotificationChannel
import com.kieronquinn.app.smartspacer.components.notifications.NotificationId
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement.GeofenceRequirementData
import com.kieronquinn.app.smartspacer.model.database.RequirementDataType
import com.kieronquinn.app.smartspacer.receivers.GeofenceReceiver
import com.kieronquinn.app.smartspacer.utils.extensions.PendingIntent_MUTABLE_FLAGS
import com.kieronquinn.app.smartspacer.utils.extensions.firstNotNull
import com.kieronquinn.app.smartspacer.utils.extensions.geocode
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface GeofenceRepository {

    val geofenceRequirements: StateFlow<List<GeofenceRequirementData>?>
    var lastLocation: LatLng?

    fun updateLastLocation(location: LatLng)
    fun registerGeofences(geofences: List<GeofenceRequirementData>? = geofenceRequirements.value)

    suspend fun geocodeLocation(location: LatLng): Address?

    fun hasLocationPermission(): Boolean
    fun hasBackgroundLocationPermission(): Boolean

    suspend fun isGeofenceLimitReached(): Boolean

}

class GeofenceRepositoryImpl(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    dataRepository: DataRepository,
    private val scope: CoroutineScope = MainScope()
): GeofenceRepository {

    companion object {
        private const val PENDING_INTENT_GEOFENCE = 1001
        private const val MAX_GEOFENCE_COUNT = 100
    }

    private val geofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    private val geofencePendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            PENDING_INTENT_GEOFENCE,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent_MUTABLE_FLAGS
        )
    }

    override val geofenceRequirements = dataRepository.getRequirementData(
        RequirementDataType.GEOFENCE, GeofenceRequirementData::class.java
    ).flowOn(Dispatchers.IO).stateIn(scope, SharingStarted.Eagerly, null)

    override var lastLocation: LatLng? = null

    override fun updateLastLocation(location: LatLng) {
        lastLocation = location
    }

    override suspend fun geocodeLocation(location: LatLng): Address? {
        return context.geocode(location)
    }

    override fun hasBackgroundLocationPermission(): Boolean {
        return context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    override fun hasLocationPermission(): Boolean {
        return context.hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    override fun registerGeofences(geofences: List<GeofenceRequirementData>?) {
        if(geofences == null) return
        //Clear all existing geofences
        geofencingClient.removeGeofences(geofencePendingIntent)
        if(geofences.isEmpty()) return
        if(!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
            showLocationPermissionNotification()
            return
        }
        val request = GeofencingRequest.Builder().apply {
            geofences.forEach { data ->
                addGeofence(data.getGeofenceRequest())
            }
            setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or
                        GeofencingRequest.INITIAL_TRIGGER_EXIT or
                        GeofencingRequest.INITIAL_TRIGGER_DWELL
            )
        }.build()
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
        }catch (e: SecurityException){
            showLocationPermissionNotification()
        }
    }

    private fun showLocationPermissionNotification() {
        notificationRepository.showNotification(
            NotificationId.BACKGROUND_LOCATION,
            NotificationChannel.ERROR
        ){
            it.setSmallIcon(R.drawable.ic_notification)
            it.setContentTitle(context.getString(R.string.notification_location_permission_required_title))
            it.setContentText(
                context.getString(R.string.notification_location_permission_required_content)
            )
            it.setContentIntent(PendingIntent.getActivity(
                context,
                NotificationId.BACKGROUND_LOCATION.ordinal,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                },
                PendingIntent.FLAG_IMMUTABLE
            ))
            it.setAutoCancel(true)
            it.priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun setupGeofences() = scope.launch {
        geofenceRequirements.filterNotNull().collect {
            registerGeofences(it)
        }
    }

    init {
        setupGeofences()
    }

    override suspend fun isGeofenceLimitReached(): Boolean {
        return geofenceRequirements.firstNotNull().size >= MAX_GEOFENCE_COUNT
    }

}