package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData

class CardTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: SubCardTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<SubCardTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_card
    override val viewType = ViewType.TEMPLATE_CARD

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
        template.subCardIcon.let {
            remoteViews.setImageViewIcon(R.id.smartspace_view_card_icon, it.tintIfNeeded(textColour))
        }
        template.subCardText.let {
            remoteViews.setTextViewText(R.id.smartspace_view_card_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_card_text, textColour)
        }
    }

}