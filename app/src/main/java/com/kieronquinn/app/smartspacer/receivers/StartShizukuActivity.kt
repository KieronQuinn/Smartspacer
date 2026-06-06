package com.kieronquinn.app.smartspacer.receivers

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.smartspacer.utils.extensions.getShizukuInstallIntent
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID

class StartShizukuActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchIntent = if(packageManager.isPackageInstalled(MANAGER_APPLICATION_ID)){
            packageManager.getLaunchIntentForPackage(MANAGER_APPLICATION_ID)
        }else{
            getShizukuInstallIntent()
        }?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
        finish()
    }

}