package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData

class BasicTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: BaseTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<BaseTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_basic
    override val viewType = ViewType.TEMPLATE_BASIC

    override val supportsSubAction = true

}