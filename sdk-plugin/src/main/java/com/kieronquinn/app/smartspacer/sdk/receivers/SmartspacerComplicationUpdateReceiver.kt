package com.kieronquinn.app.smartspacer.sdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

abstract class SmartspacerComplicationUpdateReceiver: BroadcastReceiver() {

    companion object {
        private const val ACTION_COMPLICATION_UPDATE =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.REQUEST_COMPLICATION_UPDATE"
        private const val KEY_COMPLICATION_IDS = "complication_ids"
        private const val KEY_COMPLICATION_AUTHORITIES = "complication_authorities"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun sendUpdateBroadcast(
            context: Context,
            packageName: String,
            smartspacerIds: Array<String>,
            authorities: Array<String>
        ) {
            val intent = Intent(ACTION_COMPLICATION_UPDATE).apply {
                putExtra(KEY_COMPLICATION_IDS, smartspacerIds)
                putExtra(KEY_COMPLICATION_AUTHORITIES, authorities)
                `package` = packageName
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != ACTION_COMPLICATION_UPDATE) return
        val complicationIds = intent.getStringArrayExtra(KEY_COMPLICATION_IDS) ?: return
        val authorities = intent.getStringArrayExtra(KEY_COMPLICATION_AUTHORITIES) ?: return
        if(complicationIds.size != authorities.size) return
        val requestComplications = authorities.zip(complicationIds).map {
            RequestComplication(it.first, it.second)
        }
        onRequestSmartspaceComplicationUpdate(context, requestComplications)
    }

    abstract fun onRequestSmartspaceComplicationUpdate(
        context: Context, requestComplications: List<RequestComplication>
    )

    data class RequestComplication(val authority: String, val smartspacerId: String)

}