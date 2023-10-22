package com.kieronquinn.app.smartspacer.sdksample.plugin.providers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import com.kieronquinn.app.smartspacer.sdksample.BuildConfig
import com.kieronquinn.app.smartspacer.sdksample.plugin.providers.SecureBroadcastReceiver.Companion.putExtra
import java.util.*

/**
 *  [BroadcastReceiver] that checks the package who created the intent by requiring a pending
 *  intent to be attached with the creator package matching [BuildConfig.APPLICATION_ID]. If it
 *  does not match, an exception will be thrown. Use [putExtra] to add the required extra to an
 *  intent.
 */
abstract class SecureBroadcastReceiver: BroadcastReceiver() {

    companion object {
        private const val EXTRA_VERIFICATION_PENDING_INTENT = "verification_pending_intent"

        fun putExtra(context: Context, intent: Intent): Intent {
            return intent.apply {
                putExtra(
                    EXTRA_VERIFICATION_PENDING_INTENT,
                    PendingIntent.getActivity(
                        context, UUID.randomUUID().hashCode(), Intent(), PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }

    @CallSuper
    override fun onReceive(context: Context, intent: Intent?) {
        val callingPackage = intent
            ?.getParcelableExtra<PendingIntent>(EXTRA_VERIFICATION_PENDING_INTENT)
            ?.creatorPackage
        if(callingPackage != BuildConfig.APPLICATION_ID){
            throw SecurityException("Invalid calling package $callingPackage")
        }
    }

}