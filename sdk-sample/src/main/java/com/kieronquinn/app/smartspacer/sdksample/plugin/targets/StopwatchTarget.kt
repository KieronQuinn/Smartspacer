package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.annotations.LimitedNativeSupport
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver
import com.kieronquinn.app.smartspacer.sdksample.plugin.ui.activities.RequestBatteryOptimisationActivity
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Stopwatch
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Basic target showing a title, subtitle and icon, of stopwatch type. This target demonstrates
 *  how you can communicate with a foreground service to automatically update the target with
 *  new values.
 */
@OptIn(LimitedNativeSupport::class)
class StopwatchTarget: SmartspacerTargetProvider() {

    private val stopwatch = Stopwatch.getInstance()
    private var isDismissed = false

    private fun getTargets(): List<SmartspaceTarget> {
        if(isDismissed) return emptyList()
        return listOf(
            TargetTemplate.Basic(
                id = "stopwatch_target",
                componentName = ComponentName(BuildConfig.APPLICATION_ID, "stopwatch"),
                featureType = SmartspaceTarget.FEATURE_STOPWATCH,
                title = Text("Stopwatch"),
                subtitle = Text(stopwatch.stopwatchValue.value),
                icon = Icon(AndroidIcon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_target_timer_stopwatch)),
                onClick = TapAction(
                    pendingIntent = PendingIntent.getBroadcast(
                        provideContext(),
                        1001,
                        Intent("${BuildConfig.APPLICATION_ID}.STOPWATCH_CLICK").apply {
                            `package` = BuildConfig.APPLICATION_ID
                            SecureBroadcastReceiver.putExtra(provideContext(), this)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    ),
                    //Even if you're using a broadcast, you need to set this to allow locked use
                    shouldShowOnLockScreen = true
                )
            ).create().apply {
                //Hide the "Stopwatch" title on the AoD on Native Smartspace
                hideTitleOnAod = true
            }
        )
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return getTargets()
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        isDismissed = true
        if(stopwatch.stopwatchRunning.value){
            //Stop running the stopwatch now
            stopwatch.toggleStopwatch(provideContext())
        }
        notifyChange()
        return true
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            "Stopwatch Target",
            "Basic target showing a title, subtitle and icon, of stopwatch type. This target " +
                    "demonstrates how you can communicate with a foreground service to automatically" +
                    " update the target with new values.",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_timer_stopwatch),
            setupActivity = Intent(provideContext(), RequestBatteryOptimisationActivity::class.java)
        )
    }

}