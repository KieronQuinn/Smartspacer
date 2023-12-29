package com.kieronquinn.app.smartspacer.xposed

import android.app.Application.getProcessName
import android.content.Context
import android.content.Intent
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
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if(lpparam.packageName == BuildConfig.APPLICATION_ID) {
            lpparam.setupSelfHook()
            return
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

}