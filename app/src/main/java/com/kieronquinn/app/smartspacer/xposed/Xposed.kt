package com.kieronquinn.app.smartspacer.xposed

import android.content.Intent
import com.kieronquinn.app.smartspacer.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Xposed: IXposedHookLoadPackage {
    companion object {
        private const val ACTION_OVERLAY = "com.android.launcher3.WINDOW_OVERLAY"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
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
                    if(intent.action == ACTION_OVERLAY) {
                        intent.`package` = BuildConfig.APPLICATION_ID
                    }
                }
            }
        )
    }
}