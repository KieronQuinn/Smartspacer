package com.kieronquinn.app.smartspacer.ui.views.smartspace.features

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.CallSuper
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SmartspacerWidgetClickReceiver
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget.Companion.FEATURE_WEATHER
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_DOORBELL
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate.Companion.FEATURE_ALLOWLIST_IMAGE
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.takeEllipsised
import java.util.UUID

abstract class BaseFeatureSmartspaceView(
    private val targetId: String,
    open val target: SmartspaceTarget,
    open val surface: UiSurface
): SmartspaceView() {

    companion object {
        private const val DEFAULT_MAX_LENGTH = 6

        fun create(
            targetId: String,
            target: SmartspaceTarget,
            surface: UiSurface
        ): BaseFeatureSmartspaceView {
            val feature = target.featureType
            return when {
                target.featureType == FEATURE_WEATHER -> {
                    WeatherFeatureSmartspaceView(targetId, target, surface)
                }
                FEATURE_ALLOWLIST_IMAGE.contains(feature) -> {
                    CommuteTimeFeatureSmartspaceView(targetId, target, surface)
                }
                FEATURE_ALLOWLIST_DOORBELL.contains(feature) -> {
                    DoorbellFeatureSmartspaceView(targetId, target, surface)
                }
                else -> {
                    UndefinedFeatureSmartspaceView(targetId, target, surface)
                }
            }
        }
    }

    open val supportsSubAction: Boolean = false

    @CallSuper
    override fun apply(context: Context, textColour: Int, remoteViews: RemoteViews, width: Int) {
        val bestMaxLength = target.headerAction?.subtitle?.let { title ->
            val subtitle = if(supportsSubAction){
                target.baseAction?.subtitle
            }else null
            getBestMaxLength(
                context.getAvailableTextSize(width, target.hasSubAction()),
                title,
                subtitle
            )
        }
        remoteViews.setTextColor(R.id.smartspace_view_title, textColour)
        target.headerAction?.let {
            val maxLength = bestMaxLength?.first ?: DEFAULT_MAX_LENGTH
            //Don't update the text on a weather target as it clears the date
            if(target.featureType != FEATURE_WEATHER){
                remoteViews.setTextViewText(R.id.smartspace_view_title, it.title)
            }
            remoteViews.setTextViewText(
                R.id.smartspace_view_subtitle_text, it.subtitle?.takeEllipsised(maxLength)
            )
            remoteViews.setTextColor(R.id.smartspace_view_subtitle_text, textColour)
            remoteViews.setImageViewIcon(R.id.smartspace_view_subtitle_icon, it.icon
                ?.tintIfNeeded(textColour, ComplicationTemplate.shouldTint(it)))
            if(it.icon != null) {
                remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.VISIBLE)
            }else{
                remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.GONE)
            }
            if(!it.subtitle.isNullOrEmpty()) {
                remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.VISIBLE)
            }else{
                remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.GONE)
            }
        } ?: run {
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_text, View.GONE)
            remoteViews.setViewVisibility(R.id.smartspace_view_subtitle_icon, View.GONE)
        }
        remoteViews.setOnClickAction(context, R.id.smartspace_view_feature_root, target.headerAction)
        remoteViews.setOnClickAction(context, R.id.smartspace_view_title, target.headerAction)
        val action = target.baseAction
        if(supportsSubAction && (action?.subtitle?.isNotEmpty() == true || action?.icon != null)){
            val maxLength = bestMaxLength?.second ?: DEFAULT_MAX_LENGTH
            if(action.icon != null) {
                remoteViews.setImageViewIcon(
                    R.id.smartspace_view_action_icon,
                    action.icon?.tintIfNeeded(textColour, ComplicationTemplate.shouldTint(action))
                )
                remoteViews.setOnClickAction(context, R.id.smartspace_view_action_icon, action)
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.VISIBLE)
            }else{
                remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.GONE)
            }
            if(action.subtitle?.isNotEmpty() == true) {
                remoteViews.setTextViewText(
                    R.id.smartspace_view_action_text, action.subtitle?.takeEllipsised(maxLength)
                )
                remoteViews.setTextColor(R.id.smartspace_view_action_text, textColour)
                remoteViews.setOnClickAction(context, R.id.smartspace_view_action_text, action)
                remoteViews.setOnClickAction(
                    context,
                    R.id.smartspace_view_action_text,
                    target.baseAction
                )
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.VISIBLE)
            }else{
                remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.GONE)
            }
        }else{
            remoteViews.setViewVisibility(R.id.smartspace_view_action_icon, View.GONE)
            remoteViews.setViewVisibility(R.id.smartspace_view_action_text, View.GONE)
        }
    }

    private fun SmartspaceTarget.hasSubAction(): Boolean {
        return supportsSubAction && baseAction?.title != null
    }

    private fun RemoteViews.setOnClickAction(context: Context, id: Int, action: SmartspaceAction?) {
        val pendingIntentCode = UUID.randomUUID().hashCode()
        val intent = SmartspacerWidgetClickReceiver.createIntent(
            context, targetId, surface, smartspaceAction = action?.stripData()
        )
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pendingIntentCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        setOnClickPendingIntent(id, pendingIntent)
    }

    /**
     *  Removes potentially unblobable data such as icons from the action to go into the
     *  pending intent
     */
    private fun SmartspaceAction.stripData(): SmartspaceAction {
        return copy(
            icon = null,
            extras = Bundle.EMPTY,
            subItemInfo = null
        )
    }

    private fun Icon.tintIfNeeded(textColour: Int, shouldTint: Boolean) = apply {
        if(shouldTint){
            setTint(textColour)
        }else{
            setTintList(null)
        }
    }

    private fun Icon.clearTint() = apply {
        setTintList(null)
    }

}