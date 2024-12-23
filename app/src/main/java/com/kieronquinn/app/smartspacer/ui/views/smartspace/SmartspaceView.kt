package com.kieronquinn.app.smartspacer.ui.views.smartspace

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SmartspacerWidgetClickReceiver
import com.kieronquinn.app.smartspacer.receivers.WidgetListClickReceiver
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.sdk.client.utils.getEnabledDrawableOrNull
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.BaseFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.remoteviews.RemoteViewsSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.BaseTemplateSmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.setImageViewImageTintListCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class SmartspaceView(
    open val target: SmartspaceTarget,
    open val surface: UiSurface
): KoinComponent {

    companion object {
        fun fromTarget(
            target: SmartspaceTarget,
            surface: UiSurface,
            forceBasic: Boolean
        ): SmartspaceView {
            return target.remoteViews?.let {
                RemoteViewsSmartspaceView(target, surface)
            } ?: target.templateData?.let { template ->
                BaseTemplateSmartspaceView.create(
                    target.smartspaceTargetId, target, template, surface, forceBasic
                )
            } ?: BaseFeatureSmartspaceView.create(
                target.smartspaceTargetId, target, surface, forceBasic
            )
        }
    }

    abstract val layoutRes: Int
    abstract val viewType: ViewType

    private val appWidgetRepository by inject<AppWidgetRepository>()

    fun inflate(
        context: Context,
        textColour: Int,
        shadowEnabled: Boolean,
        width: Int,
        titleSize: Float,
        subtitleSize: Float,
        featureSize: Float,
        isList: Boolean,
        overflowIntent: Intent?,
        padding: Int
    ): RemoteViews {
        val availableWidth = if(isList) {
            width - context.resources.getDimensionPixelSize(R.dimen.smartspace_view_list_overflow_width) - padding - padding
        }else width - padding - padding
        return RemoteViews(context.packageName, layoutRes).apply {
            apply(
                context,
                textColour,
                shadowEnabled,
                this,
                availableWidth,
                titleSize,
                subtitleSize,
                featureSize,
                isList,
                overflowIntent,
            )
        }
    }

    abstract fun apply(
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
    )

    protected fun getBestMaxLength(
        width: Int,
        size: Float,
        shadowEnabled: Boolean,
        title: CharSequence,
        subtitle: CharSequence?
    ): Pair<Int, Int?> {
        return appWidgetRepository.getBestMaxLength(width, size, shadowEnabled, title, subtitle)
    }

    /**
     *  Subtracts the margins, icon sizes and padding from a full size widget width to give just the
     *  space available to the TextViews.
     */
    protected fun Context.getAvailableTextSize(width: Int, hasSubtitle: Boolean): Int {
        val iconSize = resources.getDimensionPixelSize(R.dimen.smartspace_view_icon_size)
        val marginSmall = resources.getDimensionPixelSize(R.dimen.smartspace_view_icon_margin)
        val marginLarge = resources.getDimensionPixelSize(R.dimen.smartspace_view_action_margin)
        val featureWidth = getFeatureWidth(this)
        return if(hasSubtitle){
            width - featureWidth - iconSize - marginSmall - marginLarge - iconSize - marginSmall - marginLarge
        }else{
            width - featureWidth - iconSize - marginSmall - marginLarge
        }.toInt()
    }

    protected fun RemoteViews.setOnClickAction(
        context: Context,
        id: Int,
        targetId: String,
        surface: UiSurface,
        action: TapAction?,
        isList: Boolean
    ) {
        val intent = SmartspacerWidgetClickReceiver.createIntent(
            context, targetId, surface, tapAction = action
        )
        setOnClickIntent(context, targetId, id, intent, isList)
    }

    protected fun RemoteViews.setOnClickIntent(
        context: Context,
        targetId: String,
        id: Int,
        intent: Intent,
        isList: Boolean
    ) {
        if(isList && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setOnClickFillInIntent(id, WidgetListClickReceiver.getIntent(intent))
        }else{
            val pendingIntentCode = listOf(targetId, id).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingIntentCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            setOnClickPendingIntent(id, pendingIntent)
        }
    }

    protected fun RemoteViews.setupOverflow(
        context: Context,
        isList: Boolean,
        iconTint: Int,
        overflowIntent: Intent?
    ) {
        if(!isList || overflowIntent == null) {
            setViewVisibility(R.id.widget_smartspacer_list_item_overflow, View.GONE)
            return
        }
        setViewVisibility(R.id.widget_smartspacer_list_item_overflow, View.VISIBLE)
        val icon = Icon.createWithResource(context, R.drawable.ic_widget_kebab).apply {
            setTint(iconTint)
        }
        setImageViewIcon(R.id.widget_smartspacer_list_item_overflow, icon)
        setImageViewImageTintListCompat(
            R.id.widget_smartspacer_list_item_overflow, ColorStateList.valueOf(iconTint)
        )
        val tapAction = TapAction(intent = overflowIntent)
        setOnClickAction(
            context,
            R.id.widget_smartspacer_list_item_overflow,
            target.smartspaceTargetId,
            surface,
            tapAction,
            true
        )
    }

    protected fun RemoteViews.setImageViewIcon(
        context: Context,
        @IdRes
        id: Int,
        icon: Icon?
    ) {
        val bitmap = icon?.getEnabledDrawableOrNull(context)?.toBitmap()
        if(bitmap != null) {
            setImageViewBitmap(id, bitmap)
        }else{
            setImageViewIcon(id, icon)
        }
    }

    open fun getFeatureWidth(context: Context): Int {
        return 0
    }

    enum class ViewType {
        TEMPLATE_BASIC,
        TEMPLATE_CARD,
        TEMPLATE_CAROUSEL,
        TEMPLATE_HEAD_TO_HEAD,
        TEMPLATE_IMAGES,
        TEMPLATE_LIST,
        TEMPLATE_WEATHER,
        FEATURE_UNDEFINED,
        FEATURE_WEATHER,
        FEATURE_COMMUTE_TIME,
        FEATURE_DOORBELL,
        REMOTE_VIEWS
    }

}