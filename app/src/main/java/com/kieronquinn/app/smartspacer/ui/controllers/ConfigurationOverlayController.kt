package com.kieronquinn.app.smartspacer.ui.controllers

import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.os.Build.VERSION
import com.google.android.gsa.overlay.controllers.OverlayController
import com.google.android.gsa.overlay.controllers.OverlaysController

abstract class ConfigurationOverlayController(service: Service) : OverlaysController(service) {

    private val mContext: Context = service

    override fun createController(
        configuration: Configuration?,
        uid: Int,
        i: Int,
        i2: Int
    ): OverlayController {
        var context = mContext
        if (VERSION.SDK_INT >= 17 && configuration != null) {
            context = context.createConfigurationContext(configuration)
        }
        return getOverlay(uid, context)
    }

    abstract fun getOverlay(uid: Int, context: Context): OverlayController

}