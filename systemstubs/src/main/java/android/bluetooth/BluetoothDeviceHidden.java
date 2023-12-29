package android.bluetooth;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(BluetoothDevice.class)
public class BluetoothDeviceHidden {

    public boolean isConnected() {
        throw new RuntimeException("Stub!");
    }

}
