package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.*

@SuppressLint("MissingPermission")
fun Context.getLocation(force: Boolean = false) = flow {
    val provider = LocationServices.getFusedLocationProviderClient(this@getLocation)
    val location = if(force){
        provider.getCurrentLocationAsFlow().first()
    }else{
        provider.getLastLocationAsFlow().first()
            ?: provider.getCurrentLocationAsFlow().first()
    }
    emit(location)
}.flowOn(Dispatchers.IO)

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.getLastLocationAsFlow() = callbackFlow {
    try {
        lastLocation.addOnSuccessListener {
            val location = it?.let {
                LatLng(it.latitude, it.longitude)
            }
            trySend(location)
        }.addOnFailureListener {
            trySend(null)
        }
    }catch (e: SecurityException){
        trySend(null)
    }
    awaitClose {
        //Task cannot be cancelled
    }
}

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.getCurrentLocationAsFlow() = callbackFlow {
    val source = CancellationTokenSource()
    try {
        getCurrentLocation(Priority.PRIORITY_LOW_POWER, source.token).addOnSuccessListener {
            val location = it?.let {
                LatLng(it.latitude, it.longitude)
            }
            trySend(location)
        }.addOnFailureListener {
            trySend(null)
        }
    }catch (e: SecurityException){
        trySend(null)
    }
    awaitClose {
        source.cancel()
    }
}

suspend fun Context.geocode(location: LatLng) = withContext(Dispatchers.IO) {
    if(!Geocoder.isPresent()) {
        return@withContext null
    }
    val geocoder = Geocoder(this@geocode, Locale.getDefault())
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        geocoder.geocode(location).first()
    }else{
        geocoder.geocodeLegacy(location)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Geocoder.geocode(location: LatLng) = callbackFlow {
    val callback = object: GeocodeListener {
        override fun onError(errorMessage: String?) {
            trySend(null)
        }

        override fun onGeocode(addresses: MutableList<Address>) {
            trySend(addresses.firstOrNull())
        }
    }
    getFromLocation(location.latitude, location.longitude, 1, callback)
    awaitClose {
        //Call cannot be cancelled
    }
}

@Suppress("DEPRECATION")
private fun Geocoder.geocodeLegacy(location: LatLng): Address? {
    return getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
}