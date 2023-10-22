package com.kieronquinn.app.smartspacer.ui.views.smartspace.templates

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SmartspacerWidgetClickReceiver
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_WEATHER
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.*
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import java.util.*
import android.graphics.drawable.Icon as AndroidIcon

abstract class BaseTemplateSmartspaceView<T: BaseTemplateData>(
    private val targetId: String,
    open val target: SmartspaceTarget,
    open val template: T,
    open val surface: UiSurface
): SmartspaceView() {

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
            surface: UiSurface
        ): BaseTemplateSmartspaceView<*> {
            return when(template){
                is HeadToHeadTemplateData -> {
                    HeadToHeadTemplateSmartspaceView(targetId, target, template, surface)
                }
                is SubListTemplateData -> {
                    ListTemplateSmartspaceView(targetId, target, template, surface)
                }
                is SubCardTemplateData -> {
                    CardTemplateSmartspaceView(targetId, target, template, surface)
                }
                is SubImageTemplateData -> {
                    ImagesTemplateSmartspaceView(targetId, target, template, surface)
                }
                is CarouselTemplateData -> {
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

    private var textColour: Int? = null
    open val supportsSubAction = false

    @CallSuper
    override fun apply(context: Context, textColour: Int, remoteViews: RemoteViews, width: Int) {
        val bestMaxLength = template.subtitleItem?.text?.text?.let { title ->
            val subtitle = if(supportsSubAction){
                template.subtitleSupplementalItem?.text?.text
            }else null
            getBestMaxLength(
                context.getAvailableTextSize(width, template.hasSubAction()),
                title,
                subtitle
            )
        }
        template.primaryItem?.text?.let {
            //Don't update the text on a weather target as it clears the date
            if(target.featureType != FEATURE_WEATHER){
                remoteViews.setTextViewText(R.id.smartspace_view_title, it.text)
            }
        }
        remoteViews.setTextColor(R.id.smartspace_view_title, textColour)
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_template_root, template.primaryItem?.tapAction
        )
        template.subtitleItem?.text?.let {
            val maxLength = bestMaxLength?.first ?: DEFAULT_MAX_LENGTH
            remoteViews.setTextViewText(
                R.id.smartspace_view_subtitle_text, it.text.takeEllipsised(maxLength)
            )
            remoteViews.setTextColor(R.id.smartspace_view_subtitle_text, textColour)
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.GONE)
        }
        template.subtitleItem?.icon?.let {
            remoteViews.setImageViewIcon(
                R.id.smartspace_view_subtitle_icon, it.tintIfNeeded(textColour)
            )
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.GONE)
        }
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_subtitle_icon, template.subtitleItem?.tapAction
        )
        remoteViews.setOnClickAction(
            context, R.id.smartspace_view_subtitle_text, template.subtitleItem?.tapAction
        )
        if(supportsSubAction) {
            template.subtitleSupplementalItem?.text?.let {
                val ems = bestMaxLength?.second ?: DEFAULT_MAX_LENGTH
                remoteViews.setTextViewText(
                    R.id.smartspace_view_action_text,
                    it.text.takeEllipsised(ems)
                )
                remoteViews.setTextColor(R.id.smartspace_view_action_text, textColour)
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.VISIBLE)
            } ?: run {
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.GONE)
            }
            template.subtitleSupplementalItem?.icon?.let {
                remoteViews.setImageViewIcon(
                    R.id.smartspace_view_action_icon, it.tintIfNeeded(textColour)
                )
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.VISIBLE)
            } ?: run {
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.GONE)
            }
            remoteViews.setOnClickAction(
                context,
                R.id.smartspace_view_action_icon,
                template.subtitleSupplementalItem?.tapAction
            )
            remoteViews.setOnClickAction(
                context,
                R.id.smartspace_view_action_text,
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
                R.id.smartspace_view_supplemental_icon, it.tintIfNeeded(textColour)
            )
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_icon, View.VISIBLE)
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_supplemental_icon, View.GONE)
        }
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_supplemental_icon,
            template.supplementalLineItem?.tapAction
        )
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_supplemental_text,
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

    protected fun RemoteViews.setOnClickAction(context: Context, id: Int, action: TapAction?) {
        val pendingIntentCode = UUID.randomUUID().hashCode()
        val intent = SmartspacerWidgetClickReceiver.createIntent(
            context, targetId, surface, tapAction = action
        )
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pendingIntentCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        setOnClickPendingIntent(id, pendingIntent)
    }

}