package com.kieronquinn.app.smartspacer.utils.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun CameraManager.toggleTorch() {
    val id = cameraIdList.firstOrNull {
        getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    } ?: return
    val current = getTorchMode() ?: return
    setTorchMode(id, !current)
}

private suspend fun CameraManager.getTorchMode() = suspendCoroutine<Boolean?> {
    val callback = object: CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            it.resume(enabled)
            unregisterTorchCallback(this)
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            super.onTorchModeUnavailable(cameraId)
            it.resume(null)
            unregisterTorchCallback(this)
        }
    }
    registerTorchCallback(callback, null)
}