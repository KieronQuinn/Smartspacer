package com.kieronquinn.app.smartspacer;

interface ISmartspacerSuiService {

    boolean ping() = 1;
    boolean isCompatible() = 2;
    void sendPrivilegedBroadcast(IBinder applicationThread, String attributionTag, in Intent intent) = 3;
    void setProcessObserver(in IBinder observer) = 4;

    boolean isRoot() = 5;
    void startActivityPrivileged(in Intent intent) = 6;

    void destroy() = 16777114;

}