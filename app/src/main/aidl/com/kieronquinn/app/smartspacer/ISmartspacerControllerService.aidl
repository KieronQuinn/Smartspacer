package com.kieronquinn.app.smartspacer;

import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceSessionId;
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig;
import android.content.pm.ParceledListSlice;

interface ISmartspacerControllerService {

    void addSavedSession(in SmartspaceConfig config, in SmartspaceSessionId sessionId);
    ParceledListSlice getSavedSessions();

}