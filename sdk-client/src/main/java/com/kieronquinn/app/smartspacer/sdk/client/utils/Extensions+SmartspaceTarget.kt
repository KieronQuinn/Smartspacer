package com.kieronquinn.app.smartspacer.sdk.client.utils

import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun SmartspaceTarget.shouldHeaderTintIcon(): Boolean {
    templateData?.subtitleItem?.icon?.shouldTint?.let {
        return it
    }
    if(featureType != SmartspaceTarget.FEATURE_WEATHER) return true
    return ComplicationTemplate.shouldTint(headerAction)
}