package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.repositories.AtAGlanceRepository
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.EXTRA_SMARTSPACER_ID
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AtAGlanceClickReceiver: BroadcastReceiver(), KoinComponent {

    companion object {
        fun createIntent(context: Context, smartspacerId: String): Intent {
            return Intent(context, AtAGlanceClickReceiver::class.java).apply {
                putExtra(EXTRA_SMARTSPACER_ID, smartspacerId)
            }
        }
    }

    private val atAGlance by inject<AtAGlanceRepository>()

    override fun onReceive(context: Context, intent: Intent) {
        val smartspacerId = intent.getStringExtra(EXTRA_SMARTSPACER_ID) ?: return
        val state = atAGlance.getState() ?: return
        if(state.clickPendingIntent != null && state.clickIntent != null) {
            SmartspacerWidgetProvider.launchFillInIntent(
                context,
                smartspacerId,
                state.clickPendingIntent,
                state.clickIntent
            )
        }
    }

}