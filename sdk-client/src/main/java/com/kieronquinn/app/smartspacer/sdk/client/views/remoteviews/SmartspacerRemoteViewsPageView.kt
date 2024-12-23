package com.kieronquinn.app.smartspacer.sdk.client.views.remoteviews

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Color
import com.kieronquinn.app.smartspacer.sdk.client.databinding.SmartspacePageRemoteviewsBinding
import com.kieronquinn.app.smartspacer.sdk.client.utils.wrap
import com.kieronquinn.app.smartspacer.sdk.client.views.base.SmartspacerBasePageView
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.utils.copy

class SmartspacerRemoteViewsPageView(
    context: Context
): SmartspacerBasePageView<SmartspacePageRemoteviewsBinding>(
    context,
    SmartspacePageRemoteviewsBinding::inflate
) {

    override suspend fun setTarget(
        target: SmartspaceTarget,
        interactionListener: SmartspaceTargetInteractionListener?,
        tintColour: Int,
        applyShadow: Boolean
    ) = with(binding.root) {
        removeAllViews()
        val widgetContext = context.applicationContext
        val host = AppWidgetHostView(widgetContext).apply {
            setAppWidget(-1, target.widget)
            updateAppWidget(
                target.remoteViews?.wrap(widgetContext, tintColour == Color.BLACK)?.copy()
            )
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(host)
    }

}