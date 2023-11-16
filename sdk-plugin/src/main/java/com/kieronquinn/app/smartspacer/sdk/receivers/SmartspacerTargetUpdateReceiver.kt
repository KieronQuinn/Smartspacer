package com.kieronquinn.app.smartspacer.sdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

abstract class SmartspacerTargetUpdateReceiver: BroadcastReceiver() {

    companion object {
        private const val ACTION_TARGET_UPDATE =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.REQUEST_TARGET_UPDATE"
        private const val KEY_TARGET_IDS = "target_ids"
        private const val KEY_TARGET_AUTHORITIES = "target_authorities"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun sendUpdateBroadcast(
            context: Context,
            packageName: String,
            smartspacerIds: Array<String>,
            authorities: Array<String>
        ) {
            val intent = Intent(ACTION_TARGET_UPDATE).apply {
                putExtra(KEY_TARGET_IDS, smartspacerIds)
                putExtra(KEY_TARGET_AUTHORITIES, authorities)
                `package` = packageName
            }
            context.sendBroadcast(intent)
        }
    }

    final override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != ACTION_TARGET_UPDATE) return
        val targetIds = intent.getStringArrayExtra(KEY_TARGET_IDS) ?: return
        val authorities = intent.getStringArrayExtra(KEY_TARGET_AUTHORITIES) ?: return
        if(targetIds.size != authorities.size) return
        val requestTargets = authorities.zip(targetIds).map {
            RequestTarget(it.first, it.second)
        }
        onRequestSmartspaceTargetUpdate(context, requestTargets)
    }

    abstract fun onRequestSmartspaceTargetUpdate(
        context: Context, requestTargets: List<RequestTarget>
    )

    data class RequestTarget(val authority: String, val smartspacerId: String)

}