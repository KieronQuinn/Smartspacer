package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.os.Bundle
import android.os.Process
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedFragment
import android.app.smartspace.uitemplatedata.TapAction as SystemTapAction

fun TapAction.toSystemTapAction(): SystemTapAction {
    return SystemTapAction.Builder(id)
        .setUserHandle(userHandle)
        .setExtras(extras)
        .setIntent(intent)
        .setPendingIntent(pendingIntent)
        .setShouldShowOnLockscreen(shouldShowOnLockScreen)
        .build()
}

fun SystemTapAction.toTapAction(): TapAction {
    return TapAction(
        id = id,
        userHandle = userHandle ?: Process.myUserHandle(),
        extras = extras ?: Bundle.EMPTY,
        intent = intent,
        pendingIntent = pendingIntent,
        shouldShowOnLockScreen = shouldShowOnLockscreen()
    )
}

fun TapAction.replaceActionWithExpanded(targetId: String): TapAction {
    return copy(
        intent = ExpandedFragment.createOpenTargetIntent(targetId),
        pendingIntent = null,
        shouldShowOnLockScreen = true
    )
}

fun TapAction.fixActionsIfNeeded(context: Context): TapAction {
    return copy(intent = intent?.fixActionsIfNeeded(context))
}