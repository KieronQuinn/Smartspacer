package com.kieronquinn.app.smartspacer.sdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants

abstract class SmartspacerVisibilityChangedReceiver: BroadcastReceiver() {

    companion object {
        private const val ACTION_VISIBILITY_CHANGED =
            "${SmartspacerConstants.SMARTSPACER_PACKAGE_NAME}.SMARTSPACE_VISIBILITY_CHANGED"
        private const val KEY_VISIBLE = "visible"
        private const val KEY_TIMESTAMP = "timestamp"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun sendVisibilityChangedBroadcast(
            context: Context,
            visible: Boolean,
            packageName: String,
            timestamp: Long
        ) {
            val intent = Intent(ACTION_VISIBILITY_CHANGED).apply {
                putExtra(KEY_VISIBLE, visible)
                putExtra(KEY_TIMESTAMP, timestamp)
                `package` = packageName
            }
            context.sendBroadcast(intent)
        }
    }

    final override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != ACTION_VISIBILITY_CHANGED) return
        val visible = intent.getBooleanExtra(KEY_VISIBLE, false)
        val timestamp = intent.getLongExtra(KEY_TIMESTAMP, 0L)
        onSmartspaceVisibilityChanged(context, visible, timestamp)
    }

    /**
     *  Called when the visibility of Smartspace has changed. The timestamp is provided in case
     *  broadcasts arrive out of order in quick succession.
     */
    abstract fun onSmartspaceVisibilityChanged(context: Context, visible: Boolean, timestamp: Long)

}