package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews.RemoteResponse
import com.kieronquinn.app.smartspacer.sdk.model.RemoteOnClickResponse
import com.kieronquinn.app.smartspacer.sdk.utils.sendSafely
import com.kieronquinn.app.smartspacer.utils.extensions.getParcelableExtraCompat
import com.kieronquinn.app.smartspacer.utils.extensions.toRemoteResponse
import com.kieronquinn.app.smartspacer.utils.extensions.verifySecurity

class WidgetListClickReceiver: BroadcastReceiver() {

    companion object {
        private const val EXTRA_INTENT = "intent"
        private const val EXTRA_REMOTE_RESPONSE = "remote_response"

        fun getIntent(extra: Intent): Intent {
            return Intent().apply {
                putExtra(EXTRA_INTENT, extra)
            }
        }

        fun getIntent(extra: RemoteResponse): Intent {
            return Intent().apply {
                putExtra(EXTRA_REMOTE_RESPONSE, extra.toRemoteResponse().toBundle())
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.verifySecurity()
        val extraIntent = intent.getParcelableExtraCompat(EXTRA_INTENT, Intent::class.java)
        val remoteResponse = intent
            .getParcelableExtraCompat(EXTRA_REMOTE_RESPONSE, Bundle::class.java)?.let {
                RemoteOnClickResponse.RemoteResponse(it)
            }
        when {
            extraIntent != null -> context.sendBroadcast(extraIntent)
            remoteResponse != null -> remoteResponse.pendingIntent?.sendSafely()
        }
    }

}