package android.app;

import android.content.pm.ParceledListSlice;
import android.os.IBinder;

public interface IActivityTaskManager {

    abstract class Stub extends android.os.Binder implements IServiceConnection {
        public static IActivityTaskManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    void registerTaskStackListener(ITaskStackListener listener);
    void unregisterTaskStackListener(ITaskStackListener listener);

    ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags,
                                                                     int userId);

}
