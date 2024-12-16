package com.kieronquinn.app.smartspacer.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.IntentSender
import com.kieronquinn.app.smartspacer.ui.views.appwidget.HeadlessAppWidgetHostView
import com.kieronquinn.app.smartspacer.utils.extensions.getIntentSenderForConfigureActivityCompatInternal
import com.kieronquinn.app.smartspacer.utils.extensions.removeListener

class HeadlessAppWidgetHost(
    context: Context,
    id: Int,
    private val onProvidersChangedListener: OnProvidersChangedListener
): AppWidgetHost(context, id) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return HeadlessAppWidgetHostView(context)
    }

    override fun onProvidersChanged() {
        onProvidersChangedListener.onProvidersChanged()
    }

    fun getIntentSenderForConfigureActivityCompat(appWidgetId: Int, intentFlags: Int): IntentSender {
        return getIntentSenderForConfigureActivityCompatInternal(appWidgetId, intentFlags)
    }

    fun destroyView(appWidgetHostView: AppWidgetHostView) {
        removeListener(appWidgetHostView)
    }

    interface OnProvidersChangedListener {
        fun onProvidersChanged()
    }

}