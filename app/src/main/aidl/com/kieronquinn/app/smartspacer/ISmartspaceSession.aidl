package com.kieronquinn.app.smartspacer;

import android.app.smartspace.SmartspaceTargetEvent;
import com.kieronquinn.app.smartspacer.ISmartspaceOnTargetsAvailableListener;

interface ISmartspaceSession {

    void notifySmartspaceEvent(in SmartspaceTargetEvent event);
    void requestSmartspaceUpdate();
    String addOnTargetsAvailableListener(in ISmartspaceOnTargetsAvailableListener listener);
    void removeOnTargetsAvailableListener(String id);
    void close();

}