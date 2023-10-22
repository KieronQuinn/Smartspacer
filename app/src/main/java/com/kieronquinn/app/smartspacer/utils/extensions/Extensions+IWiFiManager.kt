package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.content.pm.ParceledListSlice
import android.net.wifi.IWifiManager
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.util.Log
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.PACKAGE_SHELL

@Suppress("DEPRECATION")
fun IWifiManager.getPrivilegedConfiguredNetworks(
    context: Context
): ParceledListSlice<WifiConfiguration> {
    return try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val extras = bundleOf(
                    "EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE" to context.attributionSource
                )
                ParceledListSlice(
                    getPrivilegedConfiguredNetworks(PACKAGE_SHELL, context.attributionTag, extras).list
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                getPrivilegedConfiguredNetworks(PACKAGE_SHELL, context.attributionTag)
            }
            else -> {
                getPrivilegedConfiguredNetworks(PACKAGE_SHELL)
            }
        }
    }catch (e: Exception){
        Log.e("WiFiManager", "Error getting configured networks", e)
        ParceledListSlice(emptyList())
    }
}