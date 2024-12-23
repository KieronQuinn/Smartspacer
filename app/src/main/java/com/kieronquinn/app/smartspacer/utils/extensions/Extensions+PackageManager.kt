package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.*
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import com.kieronquinn.app.smartspacer.Smartspacer

fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfoCompat(packageName)
        true
    }catch (e: NameNotFoundException){
        false
    }
}

fun PackageManager.isValidIntent(intent: Intent): Boolean {
    return queryIntentActivitiesCompat(intent, 0).isNotEmpty()
}

@Suppress("DEPRECATION")
fun PackageManager.getInstalledApplications(): List<ApplicationInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
        getInstalledApplications(0)
    }
}

/**
 *  Speed hack: OEM Smartspace is - as far as I've seen - only ever used by launchers & SystemUI.
 *  We can dramatically speed up the compatibility check on devices with lots of apps by limiting
 *  scope to just launchers and the keyguard package. Apps that use the update action are not
 *  impacted by this limitation.
 */
fun PackageManager.getOemSmartspaceCandidates(): List<ApplicationInfo> {
    return listOf(getApplicationInfo(Smartspacer.PACKAGE_KEYGUARD)) + getInstalledLaunchers()
}

private fun PackageManager.getInstalledLaunchers(): List<ApplicationInfo> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    return queryIntentActivitiesCompat(launcherIntent).map {
        it.activityInfo.applicationInfo
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryIntentActivitiesCompat(intent: Intent, flags: Int = 0): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    }else{
        queryIntentActivities(intent, flags)
    }
}

fun PackageManager.getPackageLabel(packageName: String): CharSequence? {
    return try {
        getApplicationLabel(getApplicationInfo(packageName))
    }catch (e: NameNotFoundException){
        null
    }
}

fun PackageManager.getComponentLabel(componentName: ComponentName): CharSequence? {
    return try {
        getActivityInfo(componentName).loadLabel(this)
    }catch (e: NameNotFoundException){
        null
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getServiceInfo(componentName: ComponentName): ServiceInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getServiceInfo(componentName, PackageManager.ComponentInfoFlags.of(0L))
    } else {
        getServiceInfo(componentName, 0)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getApplicationInfo(packageName: String): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        getApplicationInfo(packageName, 0)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getActivityInfo(componentName: ComponentName): ActivityInfo {
    return if (Build.VERSION.SDK_INT >= 33) {
        getActivityInfo(componentName, PackageManager.ComponentInfoFlags.of(0L))
    } else {
        getActivityInfo(componentName, 0)
    }
}

fun PackageManager.getActivityInfoNoThrow(componentName: ComponentName): ActivityInfo? {
    return try {
        getActivityInfo(componentName)
    }catch (e: NameNotFoundException){
        null
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryContentProviders(intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= 33) {
        queryIntentContentProviders(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        queryIntentContentProviders(intent, 0)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryBroadcasts(intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= 33) {
        queryBroadcastReceivers(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        queryBroadcastReceivers(intent, 0)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.resolveActivityCompat(intent: Intent, flags: Int = 0): ResolveInfo? {
    return if (Build.VERSION.SDK_INT >= 33) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        resolveActivity(intent, flags)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.resolveService(intent: Intent): ResolveInfo? {
    return if (Build.VERSION.SDK_INT >= 33) {
        resolveService(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        resolveService(intent, 0)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.resolveContentProvider(authority: String): ProviderInfo? {
    return if (Build.VERSION.SDK_INT >= 33) {
        resolveContentProvider(authority, PackageManager.ComponentInfoFlags.of(0L))
    } else {
        resolveContentProvider(authority, 0)
    }
}

fun PackageManager.packageHasPermission(packageName: String, permission: String): Boolean {
    return try {
        val info = getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS)
        val flags = info.requestedPermissionsFlags?.toTypedArray() ?: return false
        val permissions = info.requestedPermissions?.zip(flags)
        permissions?.any { it.first == permission && it.second and REQUESTED_PERMISSION_GRANTED != 0 }
            ?: return false
    }catch (e: NameNotFoundException){
        false
    }
}