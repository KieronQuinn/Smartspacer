package com.kieronquinn.app.smartspacer.components.smartspace.compat

import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

/**
 *  Undefined merger: As [TargetMerger], using [SmartspaceTarget.FEATURE_UNDEFINED] as the empty
 *  target feature type, and with no split targets
 */
object TargetMergerUndefined: TargetMerger() {

    override val blankFeatureType: Int = SmartspaceTarget.FEATURE_UNDEFINED

}