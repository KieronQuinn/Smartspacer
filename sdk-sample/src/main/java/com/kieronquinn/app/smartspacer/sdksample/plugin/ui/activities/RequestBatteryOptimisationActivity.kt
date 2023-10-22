package com.kieronquinn.app.smartspacer.sdksample.plugin.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class RequestBatteryOptimisationActivity: AppCompatActivity() {

    private var hasResumed: Boolean = false

    override fun onResume() {
        super.onResume()
        val hasPermission = hasPermission()
        when {
            hasPermission -> {
                setResult(Activity.RESULT_OK)
                finish()
                return
            }
            hasResumed -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
            else -> {
                hasResumed = true
                requestPermission()
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        startActivity(intent)
    }

    private fun hasPermission(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

}