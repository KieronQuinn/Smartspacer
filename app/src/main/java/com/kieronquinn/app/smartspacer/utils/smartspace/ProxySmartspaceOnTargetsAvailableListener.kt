package com.kieronquinn.app.smartspacer.utils.smartspace

import android.app.smartspace.SmartspaceSession
import android.app.smartspace.SmartspaceTarget
import android.content.pm.ParceledListSlice
import com.kieronquinn.app.smartspacer.ISmartspaceOnTargetsAvailableListener

class ProxySmartspaceOnTargetsAvailableListener(
    private val original: ISmartspaceOnTargetsAvailableListener
): SmartspaceSession.OnTargetsAvailableListener {

    override fun onTargetsAvailable(targets: MutableList<SmartspaceTarget>) {
        original.onTargetsAvailable(ParceledListSlice(targets))
    }

}