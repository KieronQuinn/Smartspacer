package com.kieronquinn.app.smartspacer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.utils.extensions.getPlayStoreIntentForPackage
import com.kieronquinn.app.smartspacer.utils.extensions.isPackageInstalled
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID

class StartShizukuReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = if(context.packageManager.isPackageInstalled(MANAGER_APPLICATION_ID)){
            context.packageManager.getLaunchIntentForPackage(MANAGER_APPLICATION_ID)
        }else{
            context.getPlayStoreIntentForPackage(
                MANAGER_APPLICATION_ID, "https://shizuku.rikka.app/download/"
            )
        }?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)
    }

}