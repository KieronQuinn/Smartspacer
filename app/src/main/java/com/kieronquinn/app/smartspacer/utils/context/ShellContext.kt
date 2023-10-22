package com.kieronquinn.app.smartspacer.utils.context

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.ROOT_PACKAGE
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.ROOT_UID
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.SHELL_PACKAGE
import com.kieronquinn.app.smartspacer.service.SmartspacerShizukuService.Companion.SHELL_UID

class ShellContext(context: Context, private val isRoot: Boolean) : ContextWrapper(context) {

    override fun getOpPackageName(): String {
        return "uid:${if(isRoot) ROOT_UID else SHELL_UID}"
    }

    @SuppressLint("NewApi")
    override fun getAttributionSource(): AttributionSource {
        val uid = if (isRoot) ROOT_UID else SHELL_UID
        return AttributionSource.Builder(uid)
            .setPackageName(if(isRoot) ROOT_PACKAGE else SHELL_PACKAGE).build()
    }
}