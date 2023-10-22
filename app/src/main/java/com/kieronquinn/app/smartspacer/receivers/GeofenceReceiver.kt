package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.smartspacer.components.smartspace.requirements.GeofenceRequirement
import com.kieronquinn.app.smartspacer.repositories.GeofenceRepository
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceReceiver: BroadcastReceiver(), KoinComponent {

    private val geofenceRepository by inject<GeofenceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        geofencingEvent?.triggeringLocation?.let {
            geofenceRepository.updateLastLocation(LatLng(it.latitude, it.longitude))
        }
        val triggeringIds = geofencingEvent?.triggeringGeofences?.mapNotNull { it.requestId }
        triggeringIds?.forEach {
            SmartspacerRequirementProvider.notifyChange(context, GeofenceRequirement::class.java, it)
        }
    }

}