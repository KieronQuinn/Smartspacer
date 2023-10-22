package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.os.Bundle
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import android.app.smartspace.SmartspaceConfig as SystemSmartspaceConfig

/*
 *  Due to SmartspaceConfig.Builder not taking a package name, we instead have to use
 *  a context - since we are only ever using this to create our own configs, that doesn't matter
 */
fun SmartspaceConfig.toSystemSmartspaceConfig(context: Context): SystemSmartspaceConfig {
    return SystemSmartspaceConfig.Builder(context, uiSurface.surface)
        .setSmartspaceTargetCount(smartspaceTargetCount)
        .setExtras(extras ?: Bundle.EMPTY)
        .build()
}

fun SystemSmartspaceConfig.toSmartspaceConfig(): SmartspaceConfig {
    return SmartspaceConfig(smartspaceTargetCount, UiSurface.from(uiSurface), packageName, extras)
}