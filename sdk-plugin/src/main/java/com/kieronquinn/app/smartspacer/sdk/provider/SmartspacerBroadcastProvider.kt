package com.kieronquinn.app.smartspacer.sdk.provider

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableArrayListCompat
import com.kieronquinn.app.smartspacer.sdk.utils.getParcelableCompat

/**
 *  [SmartspacerBroadcastProvider] is a provider that allows you to receive regular broadcasts from
 *  other apps to your plugin, without needing to be running all the time.
 *
 *  This is designed to allow you to capture implicit broadcasts, which would otherwise require your
 *  plugin to have an always running background service. Instead, Smartspacer will (try to) keep
 *  running all the time, and send you the events - negating the need for your own service.
 *
 *  Provide the authority of a provider extending this class in either a Target or Complication to
 *  receive events when it is added.
 *
 *  Note: Since Smartspacer is inevitably still going to be killed on **some** devices due to OEM
 *  modifications, it's not *guaranteed* that this provider will receive all events, but it's
 *  better than every plugin having its own background service, battery optimisation exemptions etc.
 *
 *  If you are wanting to receive an explicit broadcast, you should still just use a regular
 *  Manifest-registered receiver.
 */
abstract class SmartspacerBroadcastProvider: BaseProvider() {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_ON_RECEIVE = "on_receive"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val METHOD_GET_CONFIG = "get_broadcast_config"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_INTENT = "intent"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_SMARTSPACER_ID = "smartspace_id"
    }

    /**
     *  Called when the BroadcastReceiver for this provider is received. [intent] is the raw,
     *  unmodified [Intent] passed to the original receiver.
     */
    abstract fun onReceive(intent: Intent)

    /**
     *  Return the [Config] for this provider, specifying the [IntentFilter]s to listen to
     *  broadcasts from. [smartspacerId] represents the ID of the Target or Complication this
     *  Provider is attached to.
     */
    abstract fun getConfig(smartspacerId: String): Config

    final override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        verifySecurity()
        return when(method){
            METHOD_ON_RECEIVE -> {
                val intent = extras?.getParcelableCompat(EXTRA_INTENT, Intent::class.java)
                    ?: return null
                onReceive(intent)
                null
            }
            METHOD_GET_CONFIG -> {
                val smartspacerId = extras?.getString(EXTRA_SMARTSPACER_ID) ?: return null
                getConfig(smartspacerId).toBundle()
            }
            else -> null
        }
    }

    data class Config(
        /**
         *  The IntentFilter(s) to listen for broadcasts for
         */
        val intentFilters: List<IntentFilter>
    ) {

        companion object {
            private const val KEY_INTENT_FILTERS = "intent_filters"
        }

        constructor(bundle: Bundle): this(
            bundle.getParcelableArrayListCompat(KEY_INTENT_FILTERS, IntentFilter::class.java)!!
        )

        fun toBundle() = bundleOf(
            KEY_INTENT_FILTERS to ArrayList<IntentFilter>(intentFilters)
        )

    }

}