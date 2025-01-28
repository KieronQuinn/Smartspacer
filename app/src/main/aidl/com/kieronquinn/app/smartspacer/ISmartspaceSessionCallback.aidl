package com.kieronquinn.app.smartspacer;

import com.kieronquinn.app.smartspacer.ISmartspaceSession;

interface ISmartspaceSessionCallback {
    void onReady(in ISmartspaceSession session);
}