package com.kieronquinn.app.smartspacer.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.judemanutd.autostarter.AutoStartPermissionHelper
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.utils.extensions.getIgnoreBatteryOptimisationsIntent

interface BatteryOptimisationRepository {

    fun getDisableBatteryOptimisationsIntent(): Intent?
    fun areOemOptimisationsAvailable(context: Context): Boolean
    fun startOemOptimisationSettings(context: Context)

}

class BatteryOptimisationRepositoryImpl(
    context: Context
): BatteryOptimisationRepository {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val autoStarter = AutoStartPermissionHelper.getInstance()

    private fun areBatteryOptimisationsDisabled(): Boolean {
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
        autoStarter.getAutoStartPermission(context, open = true, newTask = true)
    }

}