package com.kieronquinn.app.smartspacer.ui.views.smartspace

import android.content.Context
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.repositories.AppWidgetRepository
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.ui.views.smartspace.features.BaseFeatureSmartspaceView
import com.kieronquinn.app.smartspacer.ui.views.smartspace.templates.BaseTemplateSmartspaceView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class SmartspaceView: KoinComponent {

    companion object {

        fun fromTarget(target: SmartspaceTarget, surface: UiSurface): SmartspaceView {
            return target.templateData?.let { template ->
                BaseTemplateSmartspaceView.create(
                    target.smartspaceTargetId, target, template, surface
                )
            } ?: BaseFeatureSmartspaceView.create(target.smartspaceTargetId, target, surface)
        }
    }

    abstract val layoutRes: Int
    abstract val viewType: ViewType

    private val appWidgetRepository by inject<AppWidgetRepository>()

    fun inflate(context: Context, textColour: Int, width: Int): RemoteViews {
        return RemoteViews(context.packageName, layoutRes).apply {
            apply(context, textColour, this, width)
        }
    }

    protected abstract fun apply(
        context: Context,
        textColour: Int,
        remoteViews: RemoteViews,
        width: Int
    )

    protected fun getBestMaxLength(
        width: Int,
        title: CharSequence,
        subtitle: CharSequence?
    ): Pair<Int, Int?> = appWidgetRepository.getBestMaxLength(width, title, subtitle)

    /**
     *  Subtracts the margins, icon sizes and padding from a full size widget width to give just the
     *  space available to the TextViews.
     */
    protected fun Context.getAvailableTextSize(width: Int, hasSubtitle: Boolean): Int {
        val iconSize = resources.getDimensionPixelSize(R.dimen.smartspace_view_icon_size)
        val iconMargin = resources.getDimensionPixelSize(R.dimen.smartspace_view_icon_margin)
        val viewMargin = resources.getDimensionPixelSize(R.dimen.smartspace_view_margin)
        val actionMargin = resources.getDimensionPixelSize(R.dimen.smartspace_view_action_margin)
        val featureWidth = getFeatureWidth(this)
        return if(hasSubtitle){
            width - ((2 * viewMargin) + (2 * iconMargin) + (2 * iconSize) + actionMargin + featureWidth)
        }else{
            width - ((2 * viewMargin) + iconSize + iconMargin + featureWidth)
        }.toInt()
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
        FEATURE_DOORBELL
    }

}