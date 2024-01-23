package com.kieronquinn.app.smartspacer.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.toggleTorch
import com.kieronquinn.app.smartspacer.utils.extensions.whenResumed

class FlashlightToggleActivity: AppCompatActivity() {

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if(it) {
            toggleFlashlight()
        }else{
            Toast.makeText(this, R.string.target_flashlight_toast, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            })
            finish()
        }
    }

    private var hasToggled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val hasPermission = hasPermission(Manifest.permission.CAMERA)
        setShowWhenLocked(hasPermission)
        super.onCreate(savedInstanceState)
        if(hasPermission) {
            toggleFlashlight()
        }else{
            permissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun toggleFlashlight() = whenResumed {
        if(hasToggled) return@whenResumed
        hasToggled = true
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.toggleTorch()
        finish()
    }

}