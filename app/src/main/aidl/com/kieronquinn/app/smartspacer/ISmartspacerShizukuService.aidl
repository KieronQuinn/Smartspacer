package com.kieronquinn.app.smartspacer;

import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceSessionId;
import com.kieronquinn.app.smartspacer.ISmartspaceSession;
import com.kieronquinn.app.smartspacer.IAppPredictionOnTargetsAvailableListener;
import com.kieronquinn.app.smartspacer.ISmartspacerCrashListener;
import com.kieronquinn.app.smartspacer.ITaskObserver;
import com.kieronquinn.app.smartspacer.model.appshortcuts.ShortcutQueryWrapper;
import com.kieronquinn.app.smartspacer.model.appshortcuts.AppShortcutIcon;
import android.content.pm.ParceledListSlice;

interface ISmartspacerShizukuService {

    boolean ping() = 1;
    boolean isRoot() = 2;
    void setSmartspaceService(in ComponentName component, int userId, boolean killSystemUi, in List<String> killPackages) = 3;
    void clearSmartspaceService(int userId, boolean killSystemUi, in List<String> killPackages) = 4;
    ISmartspaceSession createSmartspaceSession(in SmartspaceConfig config) = 5;
    void destroySmartspaceSession(in SmartspaceSessionId sessionId) = 6;

    void createAppPredictorSession(in IAppPredictionOnTargetsAvailableListener listener) = 7;
    void destroyAppPredictorSession() = 8;

    void toggleTorch() = 9;

    void setCrashListener(in ISmartspacerCrashListener listener) = 10;

    ParceledListSlice getShortcuts(in ShortcutQueryWrapper query) = 11;
    AppShortcutIcon getAppShortcutIcon(String packageName, String shortcutId) = 12;
    void startShortcut(String packageName, String shortcutId) = 13;

    String getUserName() = 14;

    void setTaskObserver(in ITaskObserver observer) = 15;

    void grantRestrictedSettings() = 16;

    Intent resolvePendingIntent(in PendingIntent pendingIntent) = 17;

    ParceledListSlice getSavedWiFiNetworks() = 18;

    void enableBluetooth() = 19;

    void setProcessObserver(in IBinder observer) = 20;

    String proxyContentProviderGetType(in Uri uri) = 21;
    String[] proxyContentProviderGetStreamTypes(in Uri uri, String mimeTypeFilter) = 22;
    ParcelFileDescriptor proxyContentProviderOpenFile(in Uri uri, String mode) = 23;

    void createWidgetPredictorSession(in IAppPredictionOnTargetsAvailableListener listener, in Bundle extras) = 24;
    void destroyWidgetPredictorSession() = 25;

    void destroy() = 16777114;

}