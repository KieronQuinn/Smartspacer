package com.kieronquinn.app.smartspacer.ui.views.smartspace.remoteviews

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.kieronquinn.app.smartspacer.R
import com.kieronquinn.app.smartspacer.receivers.SmartspacerWidgetClickReceiver
import com.kieronquinn.app.smartspacer.sdk.client.utils.wrap
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.utils.copy
import com.kieronquinn.app.smartspacer.ui.views.smartspace.SmartspaceView
import com.kieronquinn.app.smartspacer.utils.extensions.replaceClickWithFillIntent
import com.kieronquinn.app.smartspacer.utils.extensions.stripData

class RemoteViewsSmartspaceView(
    override val target: SmartspaceTarget,
    override val surface: UiSurface
): SmartspaceView(target, surface) {

    override val viewType = ViewType.REMOTE_VIEWS
    override val layoutRes = R.layout.smartspace_view_remoteviews

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
        remoteViews.removeAllViews(R.id.smartspace_view_root)
        val remoteViewsToApply = target.remoteViews
            ?.wrap(context, textColour == Color.BLACK)
            ?.copy()
            ?.let {
                if(isList) it.replaceClickWithFillIntent() else it
            }
        remoteViews.setOnClickAction(
            context,
            R.id.smartspace_view_root,
            target.headerAction,
            isList
        )
        return remoteViews.addView(
            R.id.smartspace_view_root,
            remoteViewsToApply
        )
    }

    private fun RemoteViews.setOnClickAction(
        context: Context,
        id: Int,
        action: SmartspaceAction?,
        isList: Boolean
    ) {
        val intent = SmartspacerWidgetClickReceiver.createIntent(
            context, target.smartspaceTargetId, surface, smartspaceAction = action?.stripData()
        )
        setOnClickIntent(context, target.smartspaceTargetId, id, intent, isList)
    }

}