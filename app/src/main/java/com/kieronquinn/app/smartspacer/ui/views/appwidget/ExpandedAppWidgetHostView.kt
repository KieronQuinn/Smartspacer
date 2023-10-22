package com.kieronquinn.app.smartspacer.ui.views.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.kieronquinn.app.smartspacer.ui.views.RoundedCornersEnforcingAppWidgetHostView

class ExpandedAppWidgetHostView(context: Context): RoundedCornersEnforcingAppWidgetHostView(context) {

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
    private var widgetWidth = 0
    private var widgetHeight = 0

    var id: String? = null

    fun updateSizeIfNeeded(width: Int, height: Int) {
        val appWidgetId = appWidgetId
        widgetWidth = width
        widgetHeight = height
        val current = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val currentWidth = current.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val currentHeight = current.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if(currentWidth != width || currentHeight != height){
            val options = bundleOf(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to width,
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to height
            )
            try {
                appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            }catch (e: NullPointerException){
                //Xiaomi broke something
            }
        }
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