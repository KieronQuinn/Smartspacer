package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_WEATHER
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.BaseTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.CarouselTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.HeadToHeadTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubCardTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubImageTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.SubListTemplateData
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import android.graphics.drawable.Icon as AndroidIcon

abstract class BaseTemplateSmartspaceView<T: BaseTemplateData>(
    private val targetId: String,
    override val target: SmartspaceTarget,
    open val template: T,
    override val surface: UiSurface
): SmartspaceView(target, surface) {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_INTENT = "original_intent"
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val EXTRA_PENDING_INTENT = "original_pending_intent"

        private const val DEFAULT_MAX_LENGTH = 6

        fun create(
            targetId: String,
            target: SmartspaceTarget,
            template: BaseTemplateData,
            surface: UiSurface,
            forceBasic: Boolean
        ): BaseTemplateSmartspaceView<*> {
            return when {
                !forceBasic && template is HeadToHeadTemplateData -> {
                    HeadToHeadTemplateSmartspaceView(targetId, target, template, surface)
                }
                !forceBasic && template is SubListTemplateData -> {
                    ListTemplateSmartspaceView(targetId, target, template, surface)
                }
                !forceBasic && template is SubCardTemplateData -> {
                    CardTemplateSmartspaceView(targetId, target, template, surface)
                }
                !forceBasic && template is SubImageTemplateData -> {
                    ImagesTemplateSmartspaceView(targetId, target, template, surface)
                }
                !forceBasic && template is CarouselTemplateData -> {
                    CarouselTemplateSmartspaceView(targetId, target, template, surface)
                }
                else -> {
                    if(target.featureType == FEATURE_WEATHER){
                        WeatherTemplateSmartspaceView(targetId, target, template, surface)
                    }else {
                        BasicTemplateSmartspaceView(targetId, target, template, surface)
                    }
                }
            }
        }
    }

    open val supportsSubAction = false

    /**
     *  Swaps the static text view [normalId] for the continuously scrolling ViewFlipper of
     *  [params]' speed bucket. [prefix] is the ID prefix of the scrolling views in the layout
     *  (`<prefix>_scroll_<bucket>` for the flipper, `_a`/`_b` for its two strips, and `_1`/`_2`
     *  for the two text cells in each strip). Each strip is two cells wide, with the text in
     *  each, so sliding it by one cell (half its width) loops seamlessly.
     */
    @SuppressLint("NewApi")
    private fun RemoteViews.setupScrollingText(
        context: Context,
        prefix: String,
        params: ScrollParams,
        normalId: Int,
        text: CharSequence,
        textSize: Float,
        textColour: Int,
        isList: Boolean,
        tapAction: TapAction?
    ) {
        fun id(name: String): Int {
            return context.resources.getIdentifier(name, "id", context.packageName)
        }
        setViewVisibility(normalId, View.GONE)
        for(bucket in 1..3) {
            val visibility = if(bucket == params.bucket) View.VISIBLE else View.GONE
            setViewVisibility(id("${prefix}_scroll_$bucket"), visibility)
        }
        val flipper = id("${prefix}_scroll_${params.bucket}")
        for(strip in listOf("a", "b")) {
            val stripId = id("${prefix}_scroll_${params.bucket}_$strip")
            //Before Android 12 the strips have fixed widths set in the layout
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setViewLayoutWidth(stripId, params.cellWidth * 2f, COMPLEX_UNIT_PX)
            }
            for(cell in listOf("1", "2")) {
                val cellId = id("${prefix}_scroll_${params.bucket}_${strip}_$cell")
                setTextViewText(cellId, text)
                setTextViewTextSize(cellId, COMPLEX_UNIT_PX, textSize)
                setTextColor(cellId, textColour)
            }
        }
        setOnClickAction(context, flipper, isList, tapAction)
    }

    @CallSuper
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
        val bestMaxLength = template.subtitleItem?.text?.text?.let { title ->
            val subtitle = if(supportsSubAction){
                template.subtitleSupplementalItem?.text?.text
            }else null
            getBestMaxLength(
                context.getAvailableTextSize(width, template.hasSubAction()),
                subtitleSize,
                shadowEnabled,
                title,
                subtitle
            )
        }
        //Weather targets keep the standard views, as their date title is managed separately
        val scrollEnabled = scrollLongText && target.featureType != FEATURE_WEATHER
        var titleText: CharSequence? = null
        var titleTapAction: TapAction? = null
        //Subtitle scrolls if it's too long for the space available to it
        val subtitleScroll = template.subtitleItem?.text?.text?.takeIf { scrollEnabled }?.let {
            getScrollParams(
                context, it, subtitleSize, shadowEnabled,
                context.getAvailableTextSize(width, template.hasSubAction())
            )
        }
        template.primaryItem?.takeIf {
            it.loggingInfo?.featureType == FEATURE_WEATHER
        }?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_primary_action_text, it.text?.text)
            remoteViews.setImageViewIcon(
                context, R.id.smartspace_view_primary_action_icon, it.icon?.tintIfNeeded(textColour)
            )
            remoteViews.setViewVisibility(R.id.smartspace_view_primary_action_icon, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.smartspace_view_primary_action_text, View.VISIBLE)
            remoteViews.setTextColor(R.id.smartspace_view_primary_action_text, textColour)
            remoteViews.setTextViewTextSize(R.id.smartspace_view_primary_action_text, COMPLEX_UNIT_PX, titleSize)
            remoteViews.setOnClickAction(
                context, R.id.smartspace_view_primary_action_icon, isList, template.primaryItem?.tapAction
            )
            remoteViews.setOnClickAction(
                context, R.id.smartspace_view_primary_action_text, isList, template.primaryItem?.tapAction
            )
            template.supplementalAlarmItem?.text?.let {
                //Don't update the text on a weather target as it clears the date
                if (target.featureType != FEATURE_WEATHER) {
                    remoteViews.setTextViewText(R.id.smartspace_view_title, it.text)
                    titleText = it.text
                    titleTapAction = template.supplementalAlarmItem?.tapAction
                }
                remoteViews.setOnClickAction(
                    context, R.id.smartspace_view_root, isList, template.supplementalAlarmItem?.tapAction
                )
                remoteViews.setOnClickAction(
                    context, R.id.smartspace_view_title, isList, template.supplementalAlarmItem?.tapAction
                )
            }
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_primary_action_icon, View.GONE)
            remoteViews.setViewVisibility(R.id.smartspace_view_primary_action_text, View.GONE)
            remoteViews.setOnClickAction(
                context, R.id.smartspace_view_root, isList, template.primaryItem?.tapAction
            )
            remoteViews.setOnClickAction(
                context, R.id.smartspace_view_title, isList, template.primaryItem?.tapAction
            )
            template.primaryItem?.text?.let {
                //Don't update the text on a weather target as it clears the date
                if (target.featureType != FEATURE_WEATHER) {
                    remoteViews.setTextViewText(R.id.smartspace_view_title, it.text)
                    titleText = it.text
                    titleTapAction = template.primaryItem?.tapAction
                }
            }
        }
        remoteViews.setupOverflow(context, isList, textColour, overflowIntent)
        remoteViews.setTextColor(R.id.smartspace_view_title, textColour)
        remoteViews.setTextViewTextSize(R.id.smartspace_view_title, COMPLEX_UNIT_PX, titleSize)
        titleText?.takeIf { scrollEnabled }?.let {
            getScrollParams(
                context, it, titleSize, shadowEnabled, context.getAvailableTextSize(width, false)
            )?.let { params ->
                remoteViews.setupScrollingText(
                    context, "smartspace_view_title", params, R.id.smartspace_view_title, it, titleSize,
                    textColour, isList, titleTapAction
                )
            }
        }
        val enforcedHeightVisibility = if(isList) View.VISIBLE else View.GONE
        remoteViews.setViewVisibility(R.id.smartspace_view_enforced_height, enforcedHeightVisibility)
        template.subtitleItem?.text?.let {
            val maxLength = bestMaxLength?.first ?: DEFAULT_MAX_LENGTH
            //When scrolling, show the full text rather than a pre-truncated version
            val subtitleText = if(subtitleScroll != null) it.text else it.text.takeEllipsised(maxLength)
            remoteViews.setTextViewText(R.id.smartspace_view_subtitle_text, subtitleText)
            remoteViews.setTextViewTextSize(R.id.smartspace_view_subtitle_text, COMPLEX_UNIT_PX, subtitleSize)
            remoteViews.setTextColor(R.id.smartspace_view_subtitle_text, textColour)
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.GONE)
        }
        template.subtitleItem?.icon?.let {
            remoteViews.setImageViewIcon(
                context, R.id.smartspace_view_subtitle_icon, it.tintIfNeeded(textColour)
            )
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.GONE)
        }
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_subtitle_icon, isList, template.subtitleItem?.tapAction
        )
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_subtitle_text, isList, template.subtitleItem?.tapAction
        )
        template.subtitleItem?.text?.let { text ->
            subtitleScroll?.let {
                remoteViews.setupScrollingText(
                    context, "smartspace_view_subtitle", it, R.id.smartspace_view_subtitle_text, text.text,
                    subtitleSize, textColour, isList, template.subtitleItem?.tapAction
                )
            }
        }
        if(supportsSubAction) {
            template.subtitleSupplementalItem?.text?.let {
                val ems = bestMaxLength?.second ?: DEFAULT_MAX_LENGTH
                remoteViews.setTextViewText(
                    R.id.smartspace_view_action_text,
                    it.text.takeEllipsised(ems)
                )
                remoteViews.setTextViewTextSize(
                    R.id.smartspace_view_action_text, COMPLEX_UNIT_PX, subtitleSize
                )
                remoteViews.setTextColor(R.id.smartspace_view_action_text, textColour)
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.VISIBLE)
            } ?: run {
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.GONE)
            }
            template.subtitleSupplementalItem?.icon?.let {
                remoteViews.setImageViewIcon(
                    context, R.id.smartspace_view_action_icon, it.tintIfNeeded(textColour)
                )
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.VISIBLE)
            } ?: run {
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.GONE)
            }
            remoteViews.setOnClickAction(
                context,
                R.id.smartspace_view_action_icon,
                isList,
                template.subtitleSupplementalItem?.tapAction
            )
            remoteViews.setOnClickAction(
                context,
                R.id.smartspace_view_action_text,
                isList,
                template.subtitleSupplementalItem?.tapAction
            )
        }
        val supplementalVisibility = if(template.supplementalLineItem != null){
            View.VISIBLE
        }else{
            View.GONE
        }
        remoteViews.setViewVisibility(R.id.smartspace_view_supplemental, supplementalVisibility)
        template.supplementalLineItem?.text?.let {
            remoteViews.setTextViewText(R.id.smartspace_view_supplemental_text, it.text)
            remoteViews.setTextColor(R.id.smartspace_view_supplemental_text, textColour)
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_text, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_text, View.GONE)
        }
        template.supplementalLineItem?.icon?.let {
            remoteViews.setImageViewIcon(
                context, R.id.smartspace_view_supplemental_icon, it.tintIfNeeded(textColour)
            )
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_icon, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_icon, View.GONE)
        }
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_supplemental_icon,
            isList,
            template.supplementalLineItem?.tapAction
        )
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_supplemental_text,
            isList,
            template.supplementalLineItem?.tapAction
        )
    }

    private fun BaseTemplateData.hasSubAction(): Boolean {
        return supportsSubAction && subtitleSupplementalItem?.text?.text != null
    }

    protected fun Icon.tintIfNeeded(tintColour: Int): AndroidIcon {
        if(shouldTint){
            icon.setTint(tintColour)
        }else{
            icon.setTintList(null)
        }
        return icon
    }

    protected fun RemoteViews.setOnClickAction(
        context: Context,
        id: Int,
        isList: Boolean,
        action: TapAction?
    ) {
        setOnClickAction(context, id, targetId, surface, action, isList)
    }

}