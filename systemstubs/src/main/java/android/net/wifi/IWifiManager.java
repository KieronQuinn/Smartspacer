package android.net.wifi;

import android.content.pm.ParceledListSlice;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

/**
 * Interface that allows controlling and querying Wi-Fi connectivity.
 *
 * @noinspection deprecation
 */
public interface IWifiManager extends android.os.IInterface {

    abstract class Stub extends android.os.Binder implements android.app.IServiceConnection {
        public static IWifiManager asInterface(android.os.IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }

    //Android 12+, uses APEX-based ParceledListSlice
    @RequiresApi(31)
    com.android.wifi.x.com.android.modules.utils.ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks(String packageName, String featureId, Bundle extras);

    //Android 11
    @RequiresApi(30)
    ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks(String packageName, String featureId);

    //Android 10
    ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks(String packageName);

}
