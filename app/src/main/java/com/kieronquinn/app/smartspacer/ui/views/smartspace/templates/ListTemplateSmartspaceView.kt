package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text

class ListTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: SubListTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<SubListTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_list
    override val viewType = ViewType.TEMPLATE_LIST

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
        template.subListIcon?.let {
            remoteViews.setImageViewIcon(R.id.smartspace_view_list_icon, it.tintIfNeeded(textColour))
        }
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_list,
            isList,
            template.subListAction
        )
        val item1 = template.subListTexts.getOrNull(0)
        remoteViews.setListItem(R.id.smartspace_view_list_item_1, item1, textColour, featureSize)
        val item2 = template.subListTexts.getOrNull(1)
        remoteViews.setListItem(R.id.smartspace_view_list_item_2, item2, textColour, featureSize)
        val item3 = template.subListTexts.getOrNull(2)
        remoteViews.setListItem(R.id.smartspace_view_list_item_3, item3, textColour, featureSize)
    }

    private fun RemoteViews.setListItem(
        id: Int,
        item: Text?,
        textColour: Int,
        textSize: Float
    ) {
        val visibility = if(item != null){
            View.VISIBLE
        }else{
            View.GONE
        }
        setViewVisibility(id, visibility)
        setTextViewText(id, item?.text)
        setTextColor(id, textColour)
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_PX, textSize)
    }

}