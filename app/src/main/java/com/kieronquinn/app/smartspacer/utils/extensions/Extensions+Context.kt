package com.kieronquinn.app.smartspacer.utils.extensions

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.IApplicationThread
import android.app.IServiceConnection
import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.ContextHidden
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.repositories.SmartspacerSettingsRepository.HideSensitive
import com.kieronquinn.app.smartspacer.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import com.kieronquinn.app.smartspacer.service.SmartspacerAccessibiltyService
import com.kieronquinn.app.smartspacer.service.SmartspacerBackgroundService
import com.kieronquinn.app.smartspacer.ui.activities.MainActivity
import dalvik.system.PathClassLoader
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.min

val Context.isDarkMode: Boolean
    get() {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

fun Context.hasPermission(vararg permission: String): Boolean {
    return permission.all { checkCallingOrSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}

fun Context.createPackageContextOrNull(
    packageName: String, flags: Int = Context.CONTEXT_IGNORE_SECURITY
): Context? {
    return try {
        createPackageContext(packageName, flags)
    }catch (e: NameNotFoundException){
        null
    }
}

fun Context.getAttr(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val dimension = attributes.getDimensionPixelSize(0, 0)
    attributes.recycle()
    return dimension
}

fun Context.hasNotificationPermission(): Boolean {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

@SuppressLint("DiscouragedApi")
fun Context.getDefaultSmartspaceComponent(): ComponentName? {
    val id = resources.getIdentifier(
        "config_defaultSmartspaceService", "string", "android"
    )
    if(id == 0) return null
    val component = resources.getString(id)
    if(component.isBlank()) return null
    val componentName = ComponentName.unflattenFromString(component) ?: return null
    //Some ROMs have the value set but the wrong ASI build so check it's actually available
    return try {
        packageManager.getServiceInfo(componentName)
        componentName
    }catch (e: NameNotFoundException){
        null
    }
}

@SuppressLint("DiscouragedApi")
fun Context.getAppPredictionComponent(): ComponentName? {
    val id = resources.getIdentifier(
        "config_defaultAppPredictionService", "string", "android"
    )
    val component = resources.getString(id)
    if(component.isBlank()) return null
    val componentName = ComponentName.unflattenFromString(component) ?: return null
    //Some ROMs have the value set but the wrong ASI build so check it's actually available
    return try {
        packageManager.getServiceInfo(componentName)
        componentName
    }catch (e: NameNotFoundException){
        null
    }
}

fun <T> Context.broadcastReceiverAsFlow(
    vararg actions: String,
    map: (Intent) -> T,
    startWith: (() -> T)? = null
) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(map(intent))
        }
    }
    actions.forEach {
        registerReceiverCompat(receiver, IntentFilter(it))
    }
    startWith?.invoke()?.let {
        trySend(it)
    }
    awaitClose {
        unregisterReceiverCompat(receiver)
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.broadcastReceiverAsFlow(
    intentFilter: IntentFilter
) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    registerReceiverCompat(receiver, intentFilter)
    awaitClose {
        unregisterReceiverCompat(receiver)
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    intentFilter: IntentFilter
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)
    }else{
        registerReceiver(receiver, intentFilter)
    }
}

fun Context.unregisterReceiverCompat(receiver: BroadcastReceiver) {
    unregisterReceiver(receiver)
}

fun Context.isScreenOff(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return !powerManager.isInteractive
}

fun Context.screenOff(): Flow<Boolean> {
    return broadcastReceiverAsFlow(
        Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON,
        map = { isScreenOff() },
        startWith = { isScreenOff() }
    )
}

fun Context.isAudioPlaying(): Boolean {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.activePlaybackConfigurations.isNotEmpty()
}

fun Context.isNotificationServiceEnabled(): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(this)
        .contains(BuildConfig.APPLICATION_ID)
}

fun Context.notificationServiceEnabled() = callbackFlow {
    //Setting is hidden but used in NotificationManagerCompat so assumed safe
    val uri = Settings.Secure.getUriFor("enabled_notification_listeners")
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(isNotificationServiceEnabled())
        }
    }
    trySend(isNotificationServiceEnabled())
    contentResolver.registerContentObserver(uri, false, observer)
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}

fun Context.audioPlaying(): Flow<Boolean> {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return callbackFlow {
        val callback = object: AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                trySend(configs.isNotEmpty())
            }
        }
        audioManager.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        trySend(isAudioPlaying())
        awaitClose {
            audioManager.unregisterAudioPlaybackCallback(callback)
        }
    }
}

fun Context.lockscreenShowing(): Flow<Boolean> {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val isLockscreenShowing = {
        powerManager.isInteractive && keyguardManager.isKeyguardLocked
    }
    return broadcastReceiverAsFlow(
        Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT,
        map = {
            isLockscreenShowing()
        }
    ) {
        isLockscreenShowing()
    }
}

fun Context.isLockscreenShowing(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return powerManager.isInteractive && keyguardManager.isKeyguardLocked
}

fun Context.displayOff(): Flow<Boolean> {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    val isDisplayOff = {
        !powerManager.isInteractive
    }
    return broadcastReceiverAsFlow(
        Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT,
        map = {
            isDisplayOff()
        }
    ) {
        isDisplayOff()
    }
}

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Context.getAccessibilityIntent(accessibilityService: Class<out AccessibilityService>): Intent {
    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        val bundle = Bundle()
        val componentName = ComponentName(packageName, accessibilityService.name).flattenToString()
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    }
}

fun Context.getNotificationListenerIntent(notificationListener: Class<out NotificationListenerService>): Intent {
    return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        val bundle = Bundle()
        val componentName = ComponentName(packageName, notificationListener.name).flattenToString()
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_FRAGMENT_ARG_KEY, componentName)
        putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    }
}

//Safe to use getRunningServices for our own service
@Suppress("deprecation")
fun Context.isServiceRunning(serviceClass: Class<out Service>): Boolean {
    if(serviceClass == SmartspacerAccessibiltyService::class.java &&
        SmartspacerBackgroundService.isUsingEnhancedModeAppListener) {
        //Bypass need to enable accessibility
        return true
    }
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getRunningServices(Integer.MAX_VALUE).any {
        it?.service?.className == serviceClass.name
    }
}

fun Context.getPlayStoreIntentForPackage(packageName: String, fallbackUrl: String): Intent? {
    val playIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=$packageName")
    }
    if (packageManager.resolveActivityCompat(playIntent) != null) return playIntent
    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(fallbackUrl)
    }
    if (packageManager.resolveActivityCompat(fallbackIntent, 0) != null) return fallbackIntent
    return null
}

fun Context.shouldShowRequireSideload(): Boolean {
    return false //This is currently disabled as Google bottled it and reverted the change on 14
    return isAtLeastU() && !wasInstalledWithSession()
}

/**
 *  Apps installed with the Session-based Package Installer are exempt from restrictions. We can
 *  check if we were installed with the session based installer by checking if the install package
 *  is not the same as the regular package installer. This varies by device, so we look it up with
 *  [getPackageInstallerPackageName]
 */
@Suppress("DEPRECATION")
fun Context.wasInstalledWithSession(): Boolean {
    val packageManager = packageManager
    //No default installer set = always show UI to be safe
    val defaultInstaller = packageManager.getPackageInstallerPackageName() ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        packageManager.getInstallSourceInfo(packageName).installingPackageName != defaultInstaller
    } else {
        packageManager.getInstallerPackageName(packageName) != defaultInstaller
    }
}

//https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/packageinstaller/install/src/android/packageinstaller/install/cts/InstallSourceInfoTest.kt;l=95
@Suppress("DEPRECATION")
private fun PackageManager.getPackageInstallerPackageName(): String? {
    val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
    installerIntent.setDataAndType(Uri.parse("content://com.example/"),
        CONTENT_TYPE_APK)
    return installerIntent.resolveActivityInfo(this, PackageManager.MATCH_DEFAULT_ONLY)
        ?.packageName
}

fun Context.showAppInfo() {
    val component = ComponentName(this, MainActivity::class.java)
    val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    launcherApps.startAppDetailsActivity(
        component,
        Process.myUserHandle(),
        Rect(0, 0, 0, 0),
        ActivityOptions.makeBasic().toBundle()
    )
}

fun Context.getDefaultLauncher(): String? {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    return packageManager.resolveActivityCompat(
        homeIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName
}

fun Context.getAllLaunchers(): List<String> {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    return packageManager.queryIntentActivitiesCompat(homeIntent).map {
        it.activityInfo.packageName
    }.filterNot {
        //Smartspacer is registered as a launcher but we never want to interact with ourselves
        it == BuildConfig.APPLICATION_ID
    }
}

fun Context.dip(value: Int): Int = resources.dip(value)

fun Context.getSystemHideSensitive(): HideSensitive {
    return try {
        val hidePrivate = Settings.Secure.getInt(
            contentResolver, "lock_screen_allow_private_notifications", 1
        ) != 1
        val hideNotifications = Settings.Secure.getInt(
            contentResolver, "lock_screen_show_notifications", 1
        ) != 1
        when {
            hideNotifications -> HideSensitive.HIDE_TARGET
            hidePrivate -> HideSensitive.HIDE_CONTENTS
            else -> HideSensitive.DISABLED
        }
    }catch (e: NullPointerException){
        //Context was not initialised in time, return default (disabled)
        HideSensitive.DISABLED
    }
}

@SuppressLint("UnsafeOptInUsageError")
fun Context.getServiceDispatcher(
    serviceConnection: ServiceConnection,
    handler: Handler,
    flags: Int
): IServiceConnection {
    return if(isAtLeastU()){
        Refine.unsafeCast<ContextHidden>(this)
            .getServiceDispatcher(serviceConnection, handler, flags.toLong())
    }else{
        Refine.unsafeCast<ContextHidden>(this)
            .getServiceDispatcher(serviceConnection, handler, flags)
    }
}

fun Context.getUser(): UserHandle {
    return Refine.unsafeCast<ContextHidden>(this).user
}

fun Context.getMainThreadHandler(): Handler {
    return Refine.unsafeCast<ContextHidden>(this).mainThreadHandler
}

fun Context.getIApplicationThread(): IApplicationThread {
    return Refine.unsafeCast<ContextHidden>(this).iApplicationThread
}

fun Context.getDisplayPortraitWidth(): Int {
    return min(getDisplayWidth(), getDisplayHeight())
}

fun Context.getDisplayPortraitHeight(): Int {
    return max(getDisplayWidth(), getDisplayHeight())
}

@Suppress("DEPRECATION")
fun Context.getDisplayWidth(): Int {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        windowManager.currentWindowMetrics.bounds.width()
    }else{
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

@Suppress("DEPRECATION")
private fun Context.getDisplayHeight(): Int {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        windowManager.currentWindowMetrics.bounds.height()
    }else{
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        displayMetrics.heightPixels
    }
}

fun Context.getClassLoaderForPackage(packageName: String): ClassLoader? {
    val sourceDir = try {
        packageManager.getApplicationInfo(packageName).sourceDir
    }catch (e: NameNotFoundException){
        return null
    }
    return PathClassLoader(sourceDir, ClassLoader.getSystemClassLoader())
}

//Hidden action, but can still be received
private const val ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED"

fun Context.getDarkMode(
    scope: LifecycleCoroutineScope
) = broadcastReceiverAsFlow(IntentFilter(ACTION_CONFIGURATION_CHANGED)).map {
    isDarkMode
}.stateIn(scope, SharingStarted.Eagerly, isDarkMode)

fun Context.flashlightOn(executor: Executor) = callbackFlow {
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val callback = object: TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            trySend(enabled)
        }
    }
    cameraManager.registerTorchCallback(executor, callback)
    awaitClose {
        cameraManager.unregisterTorchCallback(callback)
    }
}

fun Context.hasFlashlight(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
}

fun Context.hasLightSensor(): Boolean {
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
}