package com.kieronquinn.app.smartspacer.sdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

abstract class SmartspacerUpdateReceiver: BroadcastReceiver() {

    companion object {
        private const val ACTION_UPDATE =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.REQUEST_SMARTSPACE_UPDATE"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun sendUpdateBroadcast(
            context: Context,
            packageName: String
        ) {
            val intent = Intent(ACTION_UPDATE).apply {
                `package` = packageName
            }
            context.sendBroadcast(intent)
        }
    }

    final override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != ACTION_UPDATE) return
        onRequestSmartspaceUpdate(context)
    }

    abstract fun onRequestSmartspaceUpdate(context: Context)

}