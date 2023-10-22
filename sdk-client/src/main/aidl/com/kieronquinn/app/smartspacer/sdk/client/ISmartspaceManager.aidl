package com.kieronquinn.app.smartspacer.sdk.client;

import com.kieronquinn.app.smartspacer.sdk.client.ISmartspaceCallback;

//Equivalent of system ISmartspaceManager but uses Bundles for the models for compatibility
interface ISmartspaceManager {

    void createSmartspaceSession(in Bundle config, in Bundle sessionId, in IBinder token) = 1;
    boolean notifySmartspaceEvent(in Bundle sessionId, in Bundle event) = 2;
    boolean requestSmartspaceUpdate(in Bundle sessionId) = 3;
    boolean registerSmartspaceUpdates(in Bundle sessionId, in ISmartspaceCallback callback) = 4;
    boolean unregisterSmartspaceUpdates(in Bundle sessionId, in ISmartspaceCallback callback) = 5;
    void destroySmartspaceSession(in Bundle sessionId) = 6;

    boolean checkCallingPermission() = 7;
    IntentSender createPermissionRequestIntentSender() = 8;

    boolean ping() = 999;

}