package com.kieronquinn.app.smartspacer.utils.extensions

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDeviceHidden
import dev.rikka.tools.refine.Refine

fun BluetoothDevice.isConnected(): Boolean {
    return Refine.unsafeCast<BluetoothDeviceHidden>(this).isConnected
}

fun BluetoothDevice.getNameOrNull(): String? {
    return try {
        name
    }catch (e: SecurityException) {
        null
    }
}