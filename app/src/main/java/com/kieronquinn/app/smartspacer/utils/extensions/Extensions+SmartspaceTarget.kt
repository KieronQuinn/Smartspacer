package com.kieronquinn.app.smartspacer.utils.extensions

import android.content.Context
import android.graphics.Color
import android.os.Build
import com.kieronquinn.app.smartspacer.components.smartspace.targets.DefaultTarget
import com.kieronquinn.app.smartspacer.model.smartspace.Target
import com.kieronquinn.app.smartspacer.repositories.WallpaperRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import org.koin.java.KoinJavaComponent
import java.util.UUID
import android.app.smartspace.SmartspaceTarget as SystemSmartspaceTarget

fun supportsUiTemplate() = Build.VERSION.SDK_INT >= 33

private fun <T> T?.clone(to: (T) -> SystemSmartspaceTarget.Builder) {
    this?.let { to.invoke(it) }
}

@Synchronized
fun SmartspaceTarget.toSystemSmartspaceTarget(uiSurface: UiSurface): SystemSmartspaceTarget {
    val from = this
    val wallpaperRepository by KoinJavaComponent.inject<WallpaperRepository>(WallpaperRepository::class.java)
    val dark = when(uiSurface){
        UiSurface.HOMESCREEN -> wallpaperRepository.homescreenWallpaperDarkTextColour.value
        UiSurface.MEDIA_DATA_MANAGER -> wallpaperRepository.homescreenWallpaperDarkTextColour.value
        UiSurface.LOCKSCREEN -> wallpaperRepository.lockscreenWallpaperDarkTextColour.value
    }
    val tintColour =  if(dark) Color.BLACK else Color.WHITE
    return SystemSmartspaceTarget.Builder(smartspaceTargetId, componentName, userHandle).apply {
        from.baseAction?.cloneWithTint(tintColour)
            ?.toSystemSmartspaceAction().clone(::setBaseAction)
        from.headerAction?.cloneWithTint(tintColour)
            ?.toSystemSmartspaceAction().clone(::setHeaderAction)
        from.creationTimeMillis.clone(::setCreationTimeMillis)
        from.expiryTimeMillis.clone(::setExpiryTimeMillis)
        from.score.clone(::setScore)
        from.actionChips.map { it.cloneWithTint(tintColour).toSystemSmartspaceAction() }
            .clone(::setActionChips)
        from.iconGrid.map { it.cloneWithTint(tintColour).toSystemSmartspaceAction() }
            .clone(::setIconGrid)
        from.featureType.clone(::setFeatureType)
        from.isSensitive.clone(::setSensitive)
        from.shouldShowExpanded.clone(::setShouldShowExpanded)
        from.sourceNotificationKey.clone(::setSourceNotificationKey)
        from.associatedSmartspaceTargetId.clone(::setAssociatedSmartspaceTargetId)
        from.sliceUri.clone(::setSliceUri)
        from.widget.clone(::setWidget)
        if(supportsUiTemplate()){
            from.templateData
                ?.toSystemBaseTemplateData(tintColour)
                .clone(::setTemplateData)
        }
    }.build()
}

fun SystemSmartspaceTarget.toSmartspaceTarget(): SmartspaceTarget {
    val shouldTint = featureType != SmartspaceTarget.FEATURE_WEATHER
    return SmartspaceTarget(
        smartspaceTargetId = smartspaceTargetId,
        headerAction = headerAction?.toSmartspaceAction(shouldTint),
        baseAction = baseAction?.toSmartspaceAction(shouldTint),
        creationTimeMillis = creationTimeMillis,
        expiryTimeMillis = expiryTimeMillis,
        score = score,
        actionChips = actionChips.map { it.toSmartspaceAction(shouldTint) },
        iconGrid = iconGrid.map { it.toSmartspaceAction(shouldTint) },
        featureType = featureType,
        isSensitive = isSensitive,
        shouldShowExpanded = shouldShowExpanded(),
        sourceNotificationKey = sourceNotificationKey,
        componentName = componentName,
        userHandle = userHandle,
        associatedSmartspaceTargetId = associatedSmartspaceTargetId,
        sliceUri = sliceUri,
        widget = widget,
        templateData = if(supportsUiTemplate()) templateData?.toBaseTemplateData() else null,
        expandedState = null,
        canBeDismissed = featureType != SmartspaceTarget.FEATURE_WEATHER
    )
}

const val TARGET_UNIQUENESS_PREFIX = "smartspacer_"

fun SmartspaceTarget.isWeather(): Boolean {
    return featureType == SmartspaceTarget.FEATURE_WEATHER
}

fun enforceSmartspacerUniqueness(originalId: String, packageName: String): String {
    if(packageName.isEmpty()) return originalId //Keep original ID for default targets
    return TARGET_UNIQUENESS_PREFIX + packageName + "_" + originalId
}

fun stripSmartspacerUniqueness(id: String): String {
    return if(id.startsWith(TARGET_UNIQUENESS_PREFIX)){
        val packagePrefixedId = id.removePrefix(TARGET_UNIQUENESS_PREFIX)
        packagePrefixedId.substring(packagePrefixedId.indexOf("_") + 1)
    }else id
}

fun SmartspaceTarget.getUniqueId(parent: Target): String {
    return if(parent.authority != DefaultTarget.AUTHORITY){
        enforceSmartspacerUniqueness(smartspaceTargetId, parent.sourcePackage)
    }else smartspaceTargetId
}

fun SmartspaceTarget.cloneWithUniqneness(parent: Target): SmartspaceTarget {
    //Enforce uniqueness on all targets except default ones, which may rely on their IDs
    return copy(
        smartspaceTargetId = getUniqueId(parent),
        templateData = templateData?.clone()
    )
}

fun SmartspaceTarget.replaceActionsWithExpanded(parent: Target?): SmartspaceTarget {
    val uniqueId = parent?.let { getUniqueId(it) } ?: UUID.randomUUID().toString()
    return copy(
        headerAction = headerAction?.replaceActionWithExpanded(uniqueId),
        templateData = templateData?.replaceActionsWithExpanded(uniqueId)
    )
}

fun SmartspaceTarget.fixActionsIfNeeded(context: Context): SmartspaceTarget {
    return copy(
        headerAction = headerAction?.fixActionsIfNeeded(context),
        baseAction = baseAction?.fixActionsIfNeeded(context),
        templateData = templateData?.fixActionsIfNeeded(context)
    )
}

/**
 *  Equivalent to our own SmartspaceTarget.equals, but uses equalsCompat for the actions since
 *  we do more robust checking than the system.
 */
fun SystemSmartspaceTarget.equalsCompat(other: Any?): Boolean {
    if(other !is SystemSmartspaceTarget) return false
    if(other.smartspaceTargetId != smartspaceTargetId) return false
    if(other.headerAction.equalsCompat(headerAction)) return false
    if(other.baseAction.equalsCompat(baseAction)) return false
    if(other.creationTimeMillis != creationTimeMillis) return false
    if(other.expiryTimeMillis != expiryTimeMillis) return false
    if(other.score != score) return false
    if(other.actionChips.equalsCompat(actionChips)) return false
    if(other.iconGrid.equalsCompat(iconGrid)) return false
    if(other.featureType != featureType) return false
    if(other.isSensitive != isSensitive) return false
    if(other.sourceNotificationKey != sourceNotificationKey) return false
    if(other.componentName != componentName) return false
    if(other.userHandle != userHandle) return false
    if(other.associatedSmartspaceTargetId != associatedSmartspaceTargetId) return false
    if(other.sliceUri != sliceUri) return false
    if(other.widget != widget) return false
    return true
}

fun List<SystemSmartspaceTarget>?.equalsCompat(other: Any?): Boolean {
    if(this == null) return false
    if(other !is List<*>) return false
    return deepEquals(other) {
        this as SystemSmartspaceTarget
        this.equalsCompat(it)
    }
}

fun SmartspaceTarget.shouldShowOnSurface(surface: UiSurface): Boolean {
    return limitToSurfaces.isEmpty() || limitToSurfaces.contains(surface)
}