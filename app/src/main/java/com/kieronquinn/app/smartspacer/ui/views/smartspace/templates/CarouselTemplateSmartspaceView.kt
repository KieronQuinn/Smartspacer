package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.content.Context
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.LayoutInflater
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData.CarouselItem
import kotlin.math.max

class CarouselTemplateSmartspaceView(
    targetId: String,
    override val target: SmartspaceTarget,
    override val template: CarouselTemplateData,
    override val surface: UiSurface
): BaseTemplateSmartspaceView<CarouselTemplateData>(targetId, target, template, surface) {

    override val layoutRes = R.layout.smartspace_view_template_carousel
    override val viewType = ViewType.TEMPLATE_CAROUSEL
    override val supportsSubAction = true

    override fun apply(
        context: Context,
        textColour: Int,
        remoteViews: RemoteViews,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float
    ) {
        super.apply(context, textColour, remoteViews, width, titleSize, subtitleSize, featureSize)
        val item1 = template.carouselItems.getOrNull(0)
        val item2 = template.carouselItems.getOrNull(1)
        val item3 = template.carouselItems.getOrNull(2)
        val item4 = template.carouselItems.getOrNull(3)
        remoteViews.setupCarouselItem(
            context,
            item1,
            textColour,
            featureSize,
            R.id.smartspace_view_carousel_column_1,
            R.id.smartspace_view_carousel_column_1_header,
            R.id.smartspace_view_carousel_column_1_icon,
            R.id.smartspace_view_carousel_column_1_footer
        )
        remoteViews.setupCarouselItem(
            context,
            item2,
            textColour,
            featureSize,
            R.id.smartspace_view_carousel_column_2,
            R.id.smartspace_view_carousel_column_2_header,
            R.id.smartspace_view_carousel_column_2_icon,
            R.id.smartspace_view_carousel_column_2_footer
        )
        remoteViews.setupCarouselItem(
            context,
            item3,
            textColour,
            featureSize,
            R.id.smartspace_view_carousel_column_3,
            R.id.smartspace_view_carousel_column_3_header,
            R.id.smartspace_view_carousel_column_3_icon,
            R.id.smartspace_view_carousel_column_3_footer
        )
        remoteViews.setupCarouselItem(
            context,
            item4,
            textColour,
            featureSize,
            R.id.smartspace_view_carousel_column_4,
            R.id.smartspace_view_carousel_column_4_header,
            R.id.smartspace_view_carousel_column_4_icon,
            R.id.smartspace_view_carousel_column_4_footer
        )
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_carousel, template.carouselAction
        )
    }

    override fun getFeatureWidth(context: Context): Int {
        val itemsWidth = template.carouselItems.sumOf {
            max(
                it.lowerText?.text?.estimateWidth(context) ?: 0,
                it.upperText?.text?.estimateWidth(context) ?: 0
            )
        }
        val featureMargin = context.resources.getDimensionPixelSize(R.dimen.margin_16)
        return itemsWidth + featureMargin
    }

    private fun CharSequence.estimateWidth(context: Context): Int {
        val textView = LayoutInflater.from(context)
            .inflate(R.layout.smartspacer_view_template_carousel_measure, null) as TextView
        textView.text = this
        textView.measure(0, 0)
        return textView.measuredWidth
    }

    private fun RemoteViews.setupCarouselItem(
        context: Context,
        item: CarouselItem?,
        textColour: Int,
        textSize: Float,
        columnId: Int,
        headerId: Int,
        iconId: Int,
        footerId: Int
    ) {
        if(item == null){
            setViewVisibility(columnId, View.GONE)
            return
        }
        item.upperText?.let {
            setTextViewText(headerId, it.text)
            setTextColor(headerId, textColour)
            setTextViewTextSize(headerId, COMPLEX_UNIT_PX, textSize)
        }
        item.image?.let {
            setImageViewIcon(iconId, it.tintIfNeeded(textColour))
        }
        item.lowerText?.let {
            setTextViewText(footerId, it.text)
            setTextColor(footerId, textColour)
            setTextViewTextSize(footerId, COMPLEX_UNIT_PX, textSize)
        }
        setOnClickAction(context, headerId, item.tapAction)
        setOnClickAction(context, iconId, item.tapAction)
        setOnClickAction(context, footerId, item.tapAction)
    }

}