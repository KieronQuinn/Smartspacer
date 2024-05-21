package android.app;

import android.app.ActivityOptionsHidden.SceneTransitionInfo;
import android.app.servertransaction.PendingTransactionActions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import com.android.internal.content.ReferrerIntent;

import java.util.List;

public class ActivityThread {

    public static final class ActivityClientRecord {
        public IBinder token;
        public IBinder assistToken;
        public LoadedApk packageInfo;
    }

    public static ActivityThread currentActivityThread() {
        throw new RuntimeException("Stub!");
    }

    public final ActivityInfo resolveActivityInfo(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public ActivityClientRecord getActivityClient(IBinder token) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public void handleStartActivity(IBinder token, PendingTransactionActions pendingActions) {
        throw new RuntimeException("Stub!");
    }

    public void handleStartActivity(ActivityClientRecord r, PendingTransactionActions pendingActions) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = U (there is an overlap)
    @RequiresApi(Build.VERSION_CODES.S)
    public void handleStartActivity(ActivityClientRecord r, PendingTransactionActions pendingActions, ActivityOptions activityOptions) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public void handleStartActivity(ActivityClientRecord r, PendingTransactionActions pendingActions, SceneTransitionInfo sceneTransitionInfo) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public ActivityClientRecord performResumeActivity(IBinder token, boolean finalStateRequest,
                                                      String reason) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public boolean performResumeActivity(ActivityClientRecord r, boolean finalStateRequest, String reason) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public void performRestartActivity(IBinder token, boolean start) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public boolean performDestroyActivity(ActivityClientRecord r, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(35)
    public boolean performDestroyActivity(ActivityClientRecord r, boolean finishing, boolean getNonConfigInstance, String reason) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public Activity performLaunchActivity(IBinder token, Intent customIntent) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public void performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public void performRestartActivity(ActivityClientRecord r, boolean start) {
        throw new RuntimeException("Stub!");
    }

    //Survived the change, somehow
    final void performStopActivity(IBinder token, boolean saveState, String reason) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public Bundle performPauseActivity(IBinder token, boolean finished, String reason, PendingTransactionActions pendingActions) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public Bundle performPauseActivity(ActivityClientRecord r, boolean finished, String reason, PendingTransactionActions pendingActions) {
        throw new RuntimeException("Stub!");
    }

    public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai, CompatibilityInfo compatInfo) {
        throw new RuntimeException("Stub!");
    }

    //maxApi = R
    @Deprecated
    public void handleNewIntent(IBinder token, List<ReferrerIntent> intents) {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public void handleNewIntent(ActivityClientRecord r, List arg2) {
        throw new RuntimeException("Stub!");
    }

}