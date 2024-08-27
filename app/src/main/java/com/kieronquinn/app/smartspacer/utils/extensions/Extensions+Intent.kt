package com.kieronquinn.app.smartspacer.utils.extensions

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentHidden
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.Settings
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.sdk.utils.EXTRA_EXCLUDE_FROM_SMARTSPACER
import com.kieronquinn.app.smartspacer.sdk.utils.INTENT_KEY_SECURITY_TAG
import com.kieronquinn.app.smartspacer.sdk.utils.applySecurity
import com.kieronquinn.app.smartspacer.ui.activities.ExportedSmartspaceTrampolineProxyActivity
import dev.rikka.tools.refine.Refine
import java.io.Serializable

const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
//Include as flag to shut ActivityManager up when broadcasting from Shell
const val Intent_FLAG_RECEIVER_FROM_SHELL = 0x00400000

val SMARTSPACE_EXPORTED_COMPONENT = ComponentName(
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.gsa.staticplugins.opa.smartspace.ExportedSmartspaceTrampolineActivity"
)

private val SMARTSPACE_EXPORTED_COMPONENT_VARIANT = ComponentName(
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.search.assistant.verticals.ambient.smartspace.trampolineactivity.ExportedSmartspaceTrampolineActivity"
)

val EXPORTED_WEATHER_COMPONENT = ComponentName(
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.search.weather.WeatherExportedActivity"
)

private const val PACKAGE_GOOGLE_WEATHER = "com.google.android.apps.weather"

private const val EXTRA_FEEDBACK_FEATURE_TYPE = "com.google.android.apps.search.assistant.verticals.ambient.shared.constants.SMARTSPACE_EXTRA_CONTEXTUAL_FEEDBACK_FEATURE_TYPE"

fun Intent?.isValid(context: Context): Boolean {
    if(this == null) return false
    if(context.packageManager.resolveActivityCompat(this) == null) return false
    return true
}

fun Intent.verifySecurity() {
    getParcelableExtraCompat(INTENT_KEY_SECURITY_TAG, PendingIntent::class.java)?.let {
        if(it.creatorPackage == BuildConfig.APPLICATION_ID) return
    }
    throw SecurityException("Unauthorised access")
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("DEPRECATION")
fun <T: Parcelable> Intent.getParcelableExtraCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, type)
    } else {
        getParcelableExtra(key)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("DEPRECATION")
fun <T: Serializable> Intent.getSerializableExtraCompat(key: String, type: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, type)
    } else {
        getSerializableExtra(key) as? T
    }
}

val PendingIntent_MUTABLE_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
} else {
    PendingIntent.FLAG_UPDATE_CURRENT
}

fun Intent.fixActionsIfNeeded(context: Context): Intent {
    //Prevent plugins injecting this special extra
    removeExtra(EXTRA_EXCLUDE_FROM_SMARTSPACER)
    return when (component) {
        SMARTSPACE_EXPORTED_COMPONENT, SMARTSPACE_EXPORTED_COMPONENT_VARIANT -> {
            if(hasExtra(EXTRA_FEEDBACK_FEATURE_TYPE)) {
                //This is a feedback intent, return it in its current state, but tag as excluded
                putExtra(EXTRA_EXCLUDE_FROM_SMARTSPACER, true)
                return this
            }
            component = ComponentName(
                context,
                ExportedSmartspaceTrampolineProxyActivity::class.java
            )
            applySecurity(context)
            this
        }
        else -> this
    }
}

fun Intent.prepareToLeaveProcess(context: Context) {
    Refine.unsafeCast<IntentHidden>(this).prepareToLeaveProcess(context)
}

fun getSearchIntent(packageName: String): Intent {
    return Intent(Intent.ACTION_WEB_SEARCH).apply {
        `package` = packageName
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@SuppressLint("BatteryLife")
fun getIgnoreBatteryOptimisationsIntent(): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}

fun Intent.setClassLoaderToPackage(context: Context, packageName: String) {
    setExtrasClassLoader(context.getClassLoaderForPackage(packageName) ?: return)
}

fun Context.getGoogleWeatherIntent(): Intent {
    return packageManager.getLaunchIntentForPackage(PACKAGE_GOOGLE_WEATHER) ?: Intent().apply {
        component = EXPORTED_WEATHER_COMPONENT
    }
}