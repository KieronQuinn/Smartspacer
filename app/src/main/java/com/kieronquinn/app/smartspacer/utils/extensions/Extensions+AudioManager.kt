package com.kieronquinn.app.smartspacer.utils.extensions

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private val AUDIO_TYPES_HEADSET = arrayOf(
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_USB_HEADSET
)

fun AudioManager.areHeadphonesConnected(): Boolean {
    return getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
        AUDIO_TYPES_HEADSET.contains(it.type)
    } != null
}

fun AudioManager.getHeadphonesConnected() = callbackFlow {
    val callback = object: AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            trySend(areHeadphonesConnected())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            trySend(areHeadphonesConnected())
        }
    }
    registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
    awaitClose {
        unregisterAudioDeviceCallback(callback)
    }
}.distinctUntilChanged()