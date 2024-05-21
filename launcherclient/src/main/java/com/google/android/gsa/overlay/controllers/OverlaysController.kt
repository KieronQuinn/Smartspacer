package com.google.android.gsa.overlay.controllers

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import com.google.android.gsa.overlay.binders.OverlayControllerBinder
import com.google.android.libraries.launcherclient.ILauncherOverlay
import java.io.PrintWriter
import java.util.Arrays

abstract class OverlaysController(private val service: Service) {

    private val clients = SparseArray<OverlayControllerBinder>()

    val handler = Handler()
    abstract fun createController(
        configuration: Configuration?,
        uid: Int,
        i: Int,
        i2: Int
    ): OverlayController?

    @Synchronized
    open fun onBind(intent: Intent, originalBinder: ILauncherOverlay?): IBinder? {
        var iBinder: OverlayControllerBinder?
        var i = Int.MAX_VALUE
        synchronized(this) {
            val data = intent.data
            val port = data!!.port
            if (port == -1) {
                iBinder = null
            } else {
                val parseInt: Int
                if (port != Binder.getCallingUid()) {
                    Log.i(
                        "OverlaySController",
                        "Calling with an invalid UID, the interface will not work " + port + " vs " + Binder.getCallingUid()
                    )
                }
                parseInt = try {
                    data.getQueryParameter("v")!!.toInt()
                } catch (e: Exception) {
                    Log.e("OverlaySController", "Failed parsing server version")
                    i
                }
                try {
                    i = data.getQueryParameter("cv")!!.toInt()
                } catch (e2: Exception) {
                    Log.e("OverlaySController", "Client version not available ($data)")
                    i = 0
                }
                val packagesForUid = service.packageManager.getPackagesForUid(port)
                val host = data.host
                if (packagesForUid == null || !Arrays.asList(*packagesForUid).contains(host)) {
                    Log.e("OverlaySController", "Invalid uid or package")
                    iBinder = null
                } else {
                    try {
                        iBinder = clients.get(port)
                        if (!(iBinder == null || iBinder!!.mServerVersion == parseInt)) {
                            iBinder!!.destroy()
                            iBinder = null
                        }
                        if (iBinder == null) {
                            iBinder = OverlayControllerBinder(this, port, host, parseInt, i, originalBinder)
                            clients.put(port, iBinder)
                        }
                    } catch (e3: PackageManager.NameNotFoundException) {
                        Log.e("OverlaySController", "Invalid caller package")
                        iBinder = null
                    }
                }
            }
        }
        return iBinder
    }

    @Synchronized
    open fun onUnbind(intent: Intent) {
        val port = intent.data!!.port
        if (port != -1) {
            val overlayControllerBinderVar = clients[port]
            overlayControllerBinderVar?.destroy()
            clients.remove(port)
        }
    }

    @Synchronized
    fun dump(printWriter: PrintWriter) {
        printWriter.println("OverlayServiceController, num clients : " + clients.size())
        for (size in clients.size() - 1 downTo 0) {
            val overlayControllerBinder = clients.valueAt(size)
            if (overlayControllerBinder != null) {
                printWriter.println("  dump of client $size")
                val str = "    "
                printWriter.println(
                    StringBuilder(str.length + 23).append(str).append("mCallerUid: ")
                        .append(overlayControllerBinder.mCallerUid).toString()
                )
                printWriter.println(
                    StringBuilder(str.length + 27).append(str).append("mServerVersion: ")
                        .append(overlayControllerBinder.mServerVersion).toString()
                )
                printWriter.println(
                    StringBuilder(str.length + 27).append(str).append("mClientVersion: ")
                        .append(overlayControllerBinder.mClientVersion).toString()
                )
                val str2 = overlayControllerBinder.mPackageName
                printWriter.println(
                    StringBuilder(str.length + 14 + str2.toString().length).append(
                        str
                    ).append("mPackageName: ").append(str2).toString()
                )
                printWriter.println(
                    StringBuilder(str.length + 21).append(str).append("mOptions: ")
                        .append(overlayControllerBinder.mOptions).toString()
                )
                printWriter.println(
                    StringBuilder(str.length + 30).append(str).append("mLastAttachWasLandscape: ")
                        .append(overlayControllerBinder.mLastAttachWasLandscape).toString()
                )
                val baseCallbackVar = overlayControllerBinder.baseCallback
                baseCallbackVar?.dump(printWriter, str)
            } else {
                printWriter.println("  null client: $size")
            }
        }
    }

    @Synchronized
    fun onDestroy() {
        for (size in clients.size() - 1 downTo 0) {
            val overlayControllerBinderVar = clients.valueAt(size)
            overlayControllerBinderVar?.destroy()
        }
        clients.clear()
    }

}