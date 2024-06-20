package com.kieronquinn.app.smartspacer.xposed

import android.app.Application.getProcessName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.view.View
import com.kieronquinn.app.smartspacer.BuildConfig
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedSettingsProvider
import com.kieronquinn.app.smartspacer.providers.SmartspacerXposedStateProvider
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Xposed: IXposedHookLoadPackage {

    companion object {
        private const val ACTION_OVERLAY = "com.android.launcher3.WINDOW_OVERLAY"
        private const val PACKAGE_NEXUS_LAUNCHER = "com.google.android.apps.nexuslauncher"
        private const val OVERLAY_PREFIX = "com.google.android.apps.nexuslauncher.overlay"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if(lpparam.packageName == BuildConfig.APPLICATION_ID) {
            lpparam.setupSelfHook()
            return
        }
        if(lpparam.packageName == PACKAGE_NEXUS_LAUNCHER) {
            lpparam.classLoader.setupNexusLauncherSpecificHooks()
        }
        //Hook the validation call as it is called by all service binds and never changes
        XposedHelpers.findAndHookMethod(
            "android.app.ContextImpl",
            lpparam.classLoader,
            "validateServiceIntent",
            Intent::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val intent = param.args[0] as Intent
                    if(intent.action == ACTION_OVERLAY && param.thisObject.isEnabled()) {
                        intent.`package` = BuildConfig.APPLICATION_ID
                    }
                }
            }
        )
        //Hook Activity look up for new Android 15 method
        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            lpparam.classLoader,
            "resolveActivity",
            Intent::class.java,
            Integer.TYPE,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val intent = param.args[0] as Intent
                    val context = XposedHelpers.getObjectField(param.thisObject, "mContext")
                    if(intent.action == ACTION_OVERLAY && context.isEnabled()) {
                        intent.`package` = BuildConfig.APPLICATION_ID
                    }
                }
            }
        )
    }

    private fun Any.isEnabled(): Boolean {
        val context = this as Context
        return SmartspacerXposedSettingsProvider.getExpandedEnabledAndRegisterCallback(context)
    }

    private fun LoadPackageParam.setupSelfHook() {
        if(getProcessName() != "${BuildConfig.APPLICATION_ID}:xposed") return
        XposedHelpers.findAndHookMethod(
            SmartspacerXposedStateProvider::class.java.name,
            classLoader,
            "isEnabled",
            object: XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    param.result = true
                    return true
                }
            }
        )
    }

    /**
     *  When using the activity embed, the Pixel Launcher draws a solid `colorBackground` background
     *  on the overlay before minus one loads. This is fine for Discover, but creates a graphical
     *  glitch when using Smartspacer with a transparent background. Fixing this requires hooking
     *  all View inits, finding the singular View coming from a class starting with [OVERLAY_PREFIX]
     *  and replacing its onDraw with a no-op.
     */
    private fun ClassLoader.setupNexusLauncherSpecificHooks() {
        XposedHelpers.findAndHookConstructor(
            View::class.java,
            Context::class.java,
            object: XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    val clazz = param.thisObject::class.java.name
                    if(clazz.startsWith(OVERLAY_PREFIX)) {
                        this@setupNexusLauncherSpecificHooks
                            .removeCanvasBackgroundDrawingFromView(clazz)
                    }
                }
            }
        )
    }

    private fun ClassLoader.removeCanvasBackgroundDrawingFromView(className: String) {
        XposedHelpers.findAndHookMethod(
            className,
            this,
            "onDraw",
            Canvas::class.java,
            object: XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            }
        )
    }

}