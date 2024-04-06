package com.kieronquinn.app.smartspacer.ui.views.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.ViewGroup
import android.widget.RemoteViewsHidden
import com.kieronquinn.app.smartspacer.ui.views.RoundedCornersEnforcingAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.updateAppWidgetSize

class ExpandedAppWidgetHostView: RoundedCornersEnforcingAppWidgetHostView {

    constructor(context: Context): super(context)
    constructor(
        context: Context, interactionHandler: RemoteViewsHidden.InteractionHandler
    ): super(context, interactionHandler)
    constructor(
        context: Context, onClickHandler: RemoteViewsHidden.OnClickHandler
    ): super(context, onClickHandler)

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    var id: String? = null

    init {
        clipChildren = false
    }

    fun updateSizeIfNeeded(width: Float, height: Float) {
        val appWidgetId = appWidgetId
        updateAppWidgetSize(context, width, height, appWidgetManager.getAppWidgetOptions(appWidgetId))
    }

    fun removeFromParentIfNeeded() {
        (parent as? ViewGroup)?.removeView(this)
    }

    override fun setAppWidget(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        if(this.appWidgetId == appWidgetId && this.appWidgetInfo.provider == info?.provider) {
            //Trying to set the same widget, skip
            return
        }
        super.setAppWidget(appWidgetId, info)
    }

}