package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver

class SmartspaceTargetUpdateReceiver: SmartspacerTargetUpdateReceiver() {

    /**
     *  Called when Smartspacer requests an update for given list of [requestTargets], with the
     *  period defined in [SmartspacerTargetProvider.Config.refreshPeriodMinutes].
     *
     *  [requestTargets] contains the Target IDs and authorities, for routing purposes.
     */
    override fun onRequestSmartspaceTargetUpdate(
        context: Context,
        requestTargets: List<RequestTarget>
    ) {

    }

}