package android.app;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.util.List;

public interface IActivityManager extends android.os.IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection {
        public static IActivityManager asInterface(android.os.IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    //Android 11
    int broadcastIntentWithFeature(
            IApplicationThread caller,
            String callingFeatureId,
            Intent intent,
            String resolvedType,
            IIntentReceiver resultTo,
            int resultCode,
            String resultData,
            Bundle map,
            String[] requiredPermissions,
            int appOp,
            Bundle options,
            boolean serialized,
            boolean sticky,
            int userId);

    //Android 12+
    int broadcastIntentWithFeature(
            IApplicationThread caller,
            String callingFeatureId,
            Intent intent,
            String resolvedType,
            IIntentReceiver resultTo,
            int resultCode,
            String resultData,
            Bundle map,
            String[] requiredPermissions,
            String[] excludePermissions,
            int appOp,
            Bundle options,
            boolean serialized,
            boolean sticky,
            int userId);

    //Android 13+
    int broadcastIntentWithFeature(
            IApplicationThread caller,
            String callingFeatureId,
            Intent intent,
            String resolvedType,
            IIntentReceiver resultTo,
            int resultCode,
            String resultData,
            Bundle map,
            String[] requiredPermissions,
            String[] excludePermissions,
            String[] excludePackages,
            int appOp,
            Bundle options,
            boolean serialized,
            boolean sticky,
            int userId);

    void registerProcessObserver(IProcessObserver observer);
    void unregisterProcessObserver(IProcessObserver observer);
    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();

    int bindIsolatedService(
            IApplicationThread caller,
            IBinder token,
            Intent service,
            String resolvedType,
            IServiceConnection connection,
            int flags,
            String instanceName,
            String callingPackage,
            int userId);

    //Android 13
    int bindServiceInstance(
            IApplicationThread caller,
            IBinder token,
            Intent service,
            String resolvedType,
            IServiceConnection connection,
            int flags,
            String instanceName,
            String callingPackage,
            int userId);

    int startActivity(
            IApplicationThread caller,
            String callingPackage,
            Intent intent,
            String resolvedType,
            IBinder resultTo,
            String resultWho,
            int requestCode,
            int flags,
            ProfilerInfo profilerInfo,
            Bundle options
    );

    boolean unbindService(IServiceConnection serviceConnection);

    ContentProviderHolder getContentProviderExternal(
            String name,
            int userId,
            IBinder token,
            String tag
    );

    void removeContentProviderExternalAsUser(String name, IBinder token, int userId);

}
