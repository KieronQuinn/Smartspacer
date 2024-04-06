package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.content.Context
import android.content.Intent
import android.util.TypedValue
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

    override fun apply(
        context: Context,
        textColour: Int,
        shadowEnabled: Boolean,
        remoteViews: RemoteViews,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float,
        isList: Boolean,
        overflowIntent: Intent?
    ) {
        super.apply(
            context,
            textColour,
            shadowEnabled,
            remoteViews,
            width,
            titleSize,
            subtitleSize,
            featureSize,
            isList,
            overflowIntent,
        )
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_head_to_head, isList, template.headToHeadAction
        )
        template.headToHeadTitle?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_title, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_title, textColour)
            remoteViews.setTextViewTextSize(
                R.id.smartspace_view_head_to_head_title, TypedValue.COMPLEX_UNIT_PX, featureSize
            )
        }
        template.headToHeadFirstCompetitorText?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_1_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_1_text, textColour)
            remoteViews.setTextViewTextSize(
                R.id.smartspace_view_head_to_head_1_text, TypedValue.COMPLEX_UNIT_PX, featureSize
            )
        }
        template.headToHeadSecondCompetitorText?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_head_to_head_2_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_head_to_head_2_text, textColour)
            remoteViews.setTextViewTextSize(
                R.id.smartspace_view_head_to_head_2_text, TypedValue.COMPLEX_UNIT_PX, featureSize
            )
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