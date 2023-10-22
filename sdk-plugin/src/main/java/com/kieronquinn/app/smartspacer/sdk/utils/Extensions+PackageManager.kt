package com.kieronquinn.app.smartspacer.sdk.utils

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Build

@Suppress("DEPRECATION")
fun PackageManager.getProviderInfo(componentName: ComponentName): ProviderInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getProviderInfo(componentName, PackageManager.ComponentInfoFlags.of(0))
    } else {
        getProviderInfo(componentName, 0)
    }
}