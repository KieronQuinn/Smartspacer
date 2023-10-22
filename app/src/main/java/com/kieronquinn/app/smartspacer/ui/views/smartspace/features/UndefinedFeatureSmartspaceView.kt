package com.kieronquinn.app.smartspacer.ui.views.smartspace.features

import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface

class UndefinedFeatureSmartspaceView(
    targetId: String,
    target: SmartspaceTarget,
    surface: UiSurface
): BaseFeatureSmartspaceView(targetId, target, surface) {

    override val layoutRes = R.layout.smartspace_view_feature_undefined
    override val viewType = ViewType.FEATURE_UNDEFINED

    override val supportsSubAction = true

}