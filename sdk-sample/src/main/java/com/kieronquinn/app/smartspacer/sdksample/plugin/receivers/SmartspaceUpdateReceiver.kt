package com.kieronquinn.app.smartspacer.sdksample.plugin.receivers

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerUpdateReceiver

class SmartspaceUpdateReceiver: SmartspacerUpdateReceiver() {

    /**
     *  Called when *all* Smartspacer targets and complications are requested to be updated, by
     *  either Smartspace (the system) or Smartspacer's background service.
     *
     *  This can be very often (sometimes up to once a minute), consider using
     *  [SmartspacerTargetUpdateReceiver] and [SmartspacerComplicationUpdateReceiver] instead.
     */
    override fun onRequestSmartspaceUpdate(context: Context) {
        //React to update calls here
    }

}