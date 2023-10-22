package com.kieronquinn.app.smartspacer.utils.extensions

import android.net.wifi.ScanResult
import android.os.Build

@Suppress("DEPRECATION")
fun ScanResult.getSSIDCompat(): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        wifiSsid?.toString()
    } else {
        SSID
    }
}