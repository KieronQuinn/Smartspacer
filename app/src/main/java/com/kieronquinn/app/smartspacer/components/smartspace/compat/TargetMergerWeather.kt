package com.kieronquinn.app.smartspacer.components.smartspace.compat

import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget

/**
 *  Weather merger: As [TargetMerger], using [SmartspaceTarget.FEATURE_WEATHER] as the empty target
 *  feature type, and with no split targets
 */
object TargetMergerWeather: TargetMerger()