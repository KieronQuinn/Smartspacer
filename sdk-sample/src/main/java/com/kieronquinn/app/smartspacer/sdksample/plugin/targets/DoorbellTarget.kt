package com.kieronquinn.app.smartspacer.sdksample.plugin.targets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.R
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver
import com.kieronquinn.app.smartspacer.sdksample.plugin.utils.Doorbell
import android.graphics.drawable.Icon as AndroidIcon

/**
 *  Shows a target with a title, subtitle, icon and a complex view with multiple states, designed
 *  to be used as a doorbell.
 */
class DoorbellTarget: SmartspacerTargetProvider() {

    private val doorbell = Doorbell.getInstance()
    private var isDismissed = false

    private fun getTargets(): List<SmartspaceTarget> {
        if(isDismissed) return emptyList()
        val state = doorbell.getState()
        return listOf(
            TargetTemplate.Doorbell(
                id = "doorbell_target_${state.index}",
                componentName = ComponentName(
                    BuildConfig.APPLICATION_ID, "doorbell_target"
                ),
                title = Text("Doorbell Target"),
                subtitle = Text("Tap to cycle through states"),
                icon = Icon(AndroidIcon.createWithResource(
                    BuildConfig.APPLICATION_ID, R.drawable.ic_target_doorbell
                )),
                doorbellState = state,
                onClick = TapAction(
                    pendingIntent = PendingIntent.getBroadcast(
                        provideContext(),
                        1003,
                        Intent("${BuildConfig.APPLICATION_ID}.DOORBELL_CLICK").apply {
                            `package` = BuildConfig.APPLICATION_ID
                            SecureBroadcastReceiver.putExtra(provideContext(), this)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    ),
                )
            ).create().apply {
                isSensitive = true
            }
        )
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return getTargets()
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        isDismissed = true
        notifyChange()
        return true
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            "Doorbell Target",
            "Complex target with multiple states, designed to be used as a doorbell",
            AndroidIcon.createWithResource(provideContext(), R.drawable.ic_target_doorbell)
        )
    }

}