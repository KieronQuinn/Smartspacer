package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.content.Context
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData

class HeadToHeadTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: HeadToHeadTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<HeadToHeadTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_head_to_head
    override val viewType = ViewType.TEMPLATE_HEAD_TO_HEAD

    override fun apply(context: Context, textColour: Int, remoteViews: RemoteViews, width: Int) {
        super.apply(context, textColour, remoteViews, width)
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_head_to_head, template.headToHeadAction
        )
        template.headToHeadTitle?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_title, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_title, textColour)
        }
        template.headToHeadFirstCompetitorText?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_1_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_1_text, textColour)
        }
        template.headToHeadSecondCompetitorText?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_2_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_2_text, textColour)
        }
        template.headToHeadFirstCompetitorIcon?.let {
            remoteViews.setImageViewIcon(
                R.id.smartspace_view_head_to_head_1_icon, it.tintIfNeeded(textColour)
            )
        }
        template.headToHeadSecondCompetitorIcon?.let {
            remoteViews.setImageViewIcon(
                R.id.smartspace_view_head_to_head_2_icon, it.tintIfNeeded(textColour)
            )
        }
    }

}