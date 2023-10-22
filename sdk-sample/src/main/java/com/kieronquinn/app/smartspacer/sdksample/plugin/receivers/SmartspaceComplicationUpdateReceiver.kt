package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver

class SmartspaceComplicationUpdateReceiver: SmartspacerComplicationUpdateReceiver() {

    /**
     *  Called when Smartspacer requests an update for given list of [requestComplications], with
     *  the period defined in [SmartspacerComplicationProvider.Config.refreshPeriodMinutes].
     *
     *  [requestComplications] contains the Complication IDs and authorities, for routing purposes.
     */
    override fun onRequestSmartspaceComplicationUpdate(
        context: Context,
        requestComplications: List<RequestComplication>
    ) {

    }

}