package com.kieronquinn.app.smartspacer.service

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.IProcessObserver
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.kieronquinn.app.smartspacer.IRunningAppObserver
import com.kieronquinn.app.smartspacer.ISmartspacerSuiService
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.PACKAGE_SHELL
import com.kieronquinn.app.smartspacer.utils.extensions.broadcastIntentWithFeatureCompat
import com.kieronquinn.app.smartspacer.utils.extensions.getIdentifier
import com.kieronquinn.app.smartspacer.utils.extensions.packageHasPermission
import com.kieronquinn.app.smartspacer.utils.extensions.prepareToLeaveProcess
import com.topjohnwu.superuser.internal.Utils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import rikka.shizuku.SystemServiceHelper
import kotlin.system.exitProcess

@Suppress("DEPRECATION")
@SuppressLint("WrongConstant", "RestrictedApi")
class SmartspacerSuiService: ISmartspacerSuiService.Stub() {

    companion object {
        private const val PERMISSION_CAPTURE_AUDIO_HOTWORD =
            "android.permission.CAPTURE_AUDIO_HOTWORD"
    }

    private val canUseRoot = Process.myUid() == Process.ROOT_UID
    private val context = Utils.getContext()
    private val scope = MainScope()
    private var runningAppObserver: IRunningAppObserver? = null

    private val activityManager by lazy {
        val stub = SystemServiceHelper.getSystemService("activity")
        IActivityManager.Stub.asInterface(stub)
    }

    private val processObserver = object: IProcessObserver.Stub() {
        override fun onForegroundActivitiesChanged(
            pid: Int,
            uid: Int,
            foregroundActivities: Boolean
        ) {
            if(!foregroundActivities) return
            val packageName = activityManager.getPackageNameForPid(pid) ?: return
            runningAppObserver?.onRunningAppChanged(packageName)
        }

        override fun onProcessDied(pid: Int, uid: Int) {
            //No-op
        }

        override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {
            //No-op
        }

        override fun onProcessStarted(
            pid: Int,
            processUid: Int,
            packageUid: Int,
            packageName: String?,
            processName: String?
        ) {
            //No-op
        }
    }

    override fun ping(): Boolean {
        return true
    }

    override fun isCompatible(): Boolean {
        //Either root or Shell having CAPTURE_AUDIO_HOTWORD are required to access the receiver
        return canUseRoot || context.packageManager.packageHasPermission(
            PACKAGE_SHELL, PERMISSION_CAPTURE_AUDIO_HOTWORD
        )
    }

    override fun sendPrivilegedBroadcast(
        applicationThread: IBinder,
        attributionTag: String?,
        intent: Intent
    ) {
        val identifier = Process.myUserHandle().getIdentifier()
        val thread = IApplicationThread.Stub.asInterface(applicationThread)
        intent.prepareToLeaveProcess(context)
        activityManager.broadcastIntentWithFeatureCompat(
            thread, attributionTag, intent, null, identifier
        )
    }

    @Synchronized
    override fun setProcessObserver(observer: IBinder?) {
        runningAppObserver = observer?.let {
            IRunningAppObserver.Stub.asInterface(it)
        }
    }

    override fun isRoot() = canUseRoot

    override fun startActivityPrivileged(intent: Intent) {
        activityManager.startActivity(
            null,
            "android",
            intent,
            intent.resolveType(context),
            null,
            null,
            0,
            intent.flags,
            null,
            null
        )
    }

    override fun destroy() {
        scope.cancel()
        activityManager.unregisterProcessObserver(processObserver)
        exitProcess(0)
    }

    init {
        activityManager.registerProcessObserver(processObserver)
    }

    /**
     *  Finds a given PID in the running apps and returns its process name, which seems to be the
     *  package name.
     */
    private fun IActivityManager.getPackageNameForPid(pid: Int): String? {
        val rawName = runningAppProcesses.find { it.pid == pid }?.processName
        return if(rawName?.contains(":") == true){
            rawName.substring(0, rawName.indexOf(":"))
        }else rawName
    }

}