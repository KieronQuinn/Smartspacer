package com.kieronquinn.app.smartspacer.ui.activities.permission.sms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.SmsRepository
import com.kieronquinn.app.smartspacer.utils.extensions.hasPermission
import org.koin.android.ext.android.inject

class SmsPermissionActivity: AppCompatActivity() {

    private val requestSmsContract = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if(hasPermission(Manifest.permission.READ_SMS)){
            finishAndClose()
        }else{
            launchPermissions()
        }
    }

    private val smsRepository by inject<SmsRepository>()
    private var hasResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsContract.launch(Manifest.permission.READ_SMS)
    }

    override fun onResume() {
        super.onResume()
        when {
            hasPermission(Manifest.permission.READ_SMS) -> {
                finishAndClose()
                return
            }
            hasResumed -> {
                finish()
                return
            }
        }
        hasResumed = true
    }

    private fun finishAndClose() {
        smsRepository.reload()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun launchPermissions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        }
        Toast.makeText(this, R.string.complication_sms_settings_toast, Toast.LENGTH_LONG)
            .show()
        startActivity(intent)
    }

}