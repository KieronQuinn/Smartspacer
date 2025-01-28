package android.app;

import android.content.Context;
import android.content.ServiceConnection;

import java.util.concurrent.Executor;

public class LoadedApk {

    public final IServiceConnection getServiceDispatcher(ServiceConnection c,
                                                         Context context, Executor executor, long flags) {
        throw new RuntimeException("Stub!");
    }

}