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
        private const val SCROLL_GAP_DP = 32
        //The measured width is an estimate, so leave some headroom to avoid clipping the text
        private const val SCROLL_WIDTH_MARGIN = 1.05f
        private const val SCROLL_SPEED_DP = 50f
        private const val SCROLL_MIN_GAP_DP = 16
        private const val SCROLL_BASE_CELL_DP = 200

        fun fromTarget(
            target: SmartspaceTarget,
            surface: UiSurface,
            forceBasic: Boolean,
            scrollLongText: Boolean = false
        ): SmartspaceView {
            return (target.remoteViews?.let {
                RemoteViewsSmartspaceView(target, surface)
            } ?: target.templateData?.let { template ->
                BaseTemplateSmartspaceView.create(
                    target.smartspaceTargetId, target, template, surface, forceBasic
                )
            } ?: BaseFeatureSmartspaceView.create(
                target.smartspaceTargetId, target, surface, forceBasic
            )).also {
                it.scrollLongText = scrollLongText
            }
        }
    }

    /**
     *  Whether the title and subtitle of this view should scroll rather than being truncated,
     *  set from the per-Target "Scroll long text" option.
     */
    var scrollLongText: Boolean = false

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
     *  How to scroll a text: the speed [bucket] (1-3, slow to fast loop durations) and the width in
     *  pixels of one [cellWidth] (the text plus a gap) that is repeated in the scrolling strip.
     */
    protected data class ScrollParams(val bucket: Int, val cellWidth: Int)

    /**
     *  Returns how to continuously scroll [text] in [availableWidth], or null if it fits and
     *  should be shown normally.
     */
    protected fun getScrollParams(
        context: Context,
        text: CharSequence,
        size: Float,
        shadowEnabled: Boolean,
        availableWidth: Int,
        isTitle: Boolean = false
    ): ScrollParams? {
        if(availableWidth <= 0) return null
        val measuredWidth = appWidgetRepository.getTextWidth(text, size, shadowEnabled, isTitle)
        if(measuredWidth <= availableWidth) return null
        val textWidth = (measuredWidth * SCROLL_WIDTH_MARGIN).toInt()
        val density = context.resources.displayMetrics.density
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            //The strip's width can't be set at runtime before Android 12, so use the fixed width
            //strips from the layout (cells of 200, 400 and 800dp, all at the same speed) and pick
            //the smallest that fits the text and a minimum gap. Longer text is cut off at 800dp.
            val needed = textWidth + (SCROLL_MIN_GAP_DP * density).toInt()
            val bucket = (1..3).firstOrNull { (SCROLL_BASE_CELL_DP shl (it - 1)) * density >= needed } ?: 3
            return ScrollParams(bucket, ((SCROLL_BASE_CELL_DP shl (bucket - 1)) * density).toInt())
        }
        val cellWidth = textWidth + (SCROLL_GAP_DP * density).toInt()
        //Pick the closest of three loop durations (4s, 8s, 16s) to the target speed. This is only
        //approximately constant (within about 1.5x), as the durations are fixed in the animations.
        val durationMs = cellWidth / (SCROLL_SPEED_DP * density) * 1000f
        val bucket = when {
            durationMs <= 6000f -> 1
            durationMs <= 12000f -> 2
            else -> 3
        }
        return ScrollParams(bucket, cellWidth)
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
        val icon = Icon.createWithResource(context, R.drawable.ic_options).apply {
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