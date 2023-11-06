package com.kieronquinn.app.smartspacer.utils.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kieronquinn.app.smartspacer.components.smartspace.complications.DefaultComplication
import com.kieronquinn.app.smartspacer.model.smartspace.Action
import com.kieronquinn.app.smartspacer.receivers.DummyReceiver
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData.SubItemInfo
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.ui.screens.expanded.ExpandedFragment
import android.app.smartspace.SmartspaceAction as SystemSmartspaceAction

fun SmartspaceAction.toSystemSmartspaceAction(): SystemSmartspaceAction {
    val from = this
    return SystemSmartspaceAction.Builder(id, title).apply {
        from.icon.clone(::setIcon)
        from.subtitle.clone(::setSubtitle)
        from.contentDescription.clone(::setContentDescription)
        from.pendingIntent.clone(::setPendingIntent)
        from.intent.clone(::setIntent)
        from.userHandle.clone(::setUserHandle)
        from.extras.clone(::setExtras)
    }.build()
}

fun SystemSmartspaceAction.toSmartspaceAction(shouldTint: Boolean): SmartspaceAction {
    return SmartspaceAction(
        id = id,
        icon = icon,
        title = title.toString(),
        subtitle = subtitle,
        contentDescription = contentDescription,
        pendingIntent = pendingIntent,
        intent = intent,
        extras = extras ?: Bundle.EMPTY,
        subItemInfo = toSubItemInfo(shouldTint)
    )
}

fun SystemSmartspaceAction.toSubItemInfo(shouldTint: Boolean): SubItemInfo {
    return SubItemInfo(
        text = subtitle?.let { Text(it) },
        icon = icon?.let { Icon(icon = it, shouldTint = shouldTint) },
        tapAction = TapAction(
            id = id,
            intent = intent,
            pendingIntent = pendingIntent,
            extras = extras ?: Bundle.EMPTY
        )
    )
}

private fun <T> T?.clone(to: (T) -> SystemSmartspaceAction.Builder) {
    this?.let { to.invoke(it) }
}

fun SmartspaceAction.cloneWithUniqneness(parent: Action): SmartspaceAction {
    return copy(
        id = getUniqueId(parent),
        subItemInfo = subItemInfo?.copy()
    )
}

fun SmartspaceAction.cloneWithTint(tintColour: Int, forceTint: Boolean = false): SmartspaceAction {
    return copy(
        icon = icon?.cloneWithTint(tintColour, forceTint || shouldTintIcon())
    )
}

fun SmartspaceAction.shouldTintIcon(): Boolean {
    return subItemInfo?.icon?.shouldTint ?: ComplicationTemplate.shouldTint(this)
}

fun SmartspaceAction.getUniqueId(parent: Action): String {
    //Enforce uniqueness on all targets except default ones, which may rely on their IDs
    return if(parent.authority != DefaultComplication.AUTHORITY){
        enforceSmartspacerUniqueness(id, parent.sourcePackage)
    }else id
}

fun SmartspaceAction.reformatBullet(remove: Boolean): SmartspaceAction {
    return SmartspaceAction(this).apply {
        subtitle = subtitle?.reformatBullet(remove)
    }
}

fun SmartspaceAction.replaceActionWithExpanded(targetId: String): SmartspaceAction {
    return copy(
        intent = ExpandedFragment.createOpenTargetIntent(targetId),
        pendingIntent = null
    ).apply {
        launchDisplayOnLockScreen = true
    }
}

fun SmartspaceAction.fixActionsIfNeeded(context: Context): SmartspaceAction {
    //An action with no pendingIntent or intent misbehaves, so fix that
    val fixedPendingIntent = if(pendingIntent == null && intent == null){
        createDummyPendingIntent(context)
    } else null
    return copy(
        intent = intent?.fixActionsIfNeeded(context),
        pendingIntent = fixedPendingIntent ?: pendingIntent,
        skipPendingIntent = fixedPendingIntent != null
    ).apply {
        feedbackIntent = feedbackIntent?.fixActionsIfNeeded(context)
        aboutIntent = aboutIntent?.fixActionsIfNeeded(context)
    }
}

private fun SmartspaceAction.createDummyPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DummyReceiver::class.java)
    return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent_MUTABLE_FLAGS)
}

/**
 *  Equivalent to our own SmartspaceAction.equals(), uses more robust checks than the system for
 *  equality
 */
fun SystemSmartspaceAction?.equalsCompat(other: Any?): Boolean {
    if(this == null) return false
    if(other !is SystemSmartspaceAction) return false
    if(other.id != id) return false
    if(other.title != title) return false
    if(other.subtitle != subtitle) return false
    if(other.contentDescription != contentDescription) return false
    //Intent & extras are not checked for equality as they do not have .equals()
    return true
}

fun List<SystemSmartspaceAction>?.equalsCompat(other: Any?): Boolean {
    if(this == null) return false
    if(other !is List<*>) return false
    return deepEquals(other) {
        this as SystemSmartspaceAction
        this.equalsCompat(it)
    }
}

fun SmartspaceAction.shouldShowOnSurface(surface: UiSurface): Boolean {
    return limitToSurfaces.isEmpty() || limitToSurfaces.contains(surface)
}

/**
 *  Removes potentially unblobable data such as icons from the action to go into the
 *  pending intent
 */
fun SmartspaceAction.stripData(): SmartspaceAction {
    return copy(
        icon = null,
        extras = Bundle.EMPTY,
        subItemInfo = null
    )
}