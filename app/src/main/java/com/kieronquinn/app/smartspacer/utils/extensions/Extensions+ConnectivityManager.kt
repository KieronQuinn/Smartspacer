package com.kieronquinn.app.smartspacer.utils.extensions

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun ConnectivityManager.currentWiFiNetwork() = callbackFlow {
    val listener = capabilityListener {
        trySend(it)
    }
    val currentCapabilities = activeNetwork?.let {
        getNetworkCapabilities(it)?.takeIf { capabilities ->
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
    trySend(currentCapabilities)
    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    registerNetworkCallback(request, listener)
    awaitClose {
        unregisterNetworkCallback(listener)
    }
}

private fun ConnectivityManager.capabilityListener(
    onCapabilitiesChanged: (NetworkCapabilities?) -> Unit
): ConnectivityManager.NetworkCallback {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                onCapabilitiesChanged(networkCapabilities)
            }

            override fun onAvailable(network: Network) {
                onCapabilitiesChanged(getNetworkCapabilities(network))
            }

            override fun onLost(network: Network) {
                onCapabilitiesChanged(null)
            }
        }
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                onCapabilitiesChanged(networkCapabilities)
            }
        }
    }
}