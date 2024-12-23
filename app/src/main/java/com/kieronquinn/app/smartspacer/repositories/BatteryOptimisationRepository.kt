package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.widget.Toast
import com.judemanutd.autostarter.AutoStartPermissionHelper
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.utils.extensions.getIgnoreBatteryOptimisationsIntent

interface BatteryOptimisationRepository {

    fun areBatteryOptimisationsDisabled(): Boolean
    fun getDisableBatteryOptimisationsIntent(): Intent?
    fun areOemOptimisationsAvailable(context: Context): Boolean
    fun startOemOptimisationSettings(context: Context)

}

class BatteryOptimisationRepositoryImpl(
    context: Context
): BatteryOptimisationRepository {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val autoStarter = AutoStartPermissionHelper.getInstance()

    override fun areBatteryOptimisationsDisabled(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
    }

    @SuppressLint("BatteryLife")
    override fun getDisableBatteryOptimisationsIntent(): Intent? {
        if(areBatteryOptimisationsDisabled()) return null
        return getIgnoreBatteryOptimisationsIntent()
    }

    override fun areOemOptimisationsAvailable(context: Context): Boolean {
        return autoStarter.isAutoStartPermissionAvailable(context)
    }

    override fun startOemOptimisationSettings(context: Context) {
        try {
            autoStarter.getAutoStartPermission(context, open = true, newTask = true)
        }catch (e: Exception) {
            Toast.makeText(
                context,
                R.string.notification_battery_optimisation_failed_toast,
                Toast.LENGTH_LONG
            ).show()
        }
    }

}